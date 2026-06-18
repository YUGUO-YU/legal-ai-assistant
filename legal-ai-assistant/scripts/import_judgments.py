#!/usr/bin/env python3
"""
裁判文书数据导入脚本
支持从 JSON 文件批量导入裁判文书到 Elasticsearch，并可触发向量化。

用法:
  python scripts/import_judgments.py --file data/judgments.json --es-host localhost:9200
  python scripts/import_judgments.py --file data/judgments.csv --milvus-vectorize
"""

import argparse
import json
import csv
import sys
import os
import time
from urllib.request import Request, urlopen
from urllib.error import URLError
from typing import List, Dict, Optional

DEFAULT_ES_HOST = "http://localhost:9200"
DEFAULT_ES_INDEX = "legal_judgments"
DEFAULT_BACKEND_HOST = "http://localhost:3001"
BATCH_SIZE = 100

def parse_args():
    parser = argparse.ArgumentParser(description="导入裁判文书数据")
    parser.add_argument("--file", required=True, help="输入文件路径 (JSON 或 CSV)")
    parser.add_argument("--es-host", default=DEFAULT_ES_HOST, help=f"ES 地址, 默认: {DEFAULT_ES_HOST}")
    parser.add_argument("--es-index", default=DEFAULT_ES_INDEX, help=f"ES 索引名, 默认: {DEFAULT_ES_INDEX}")
    parser.add_argument("--backend-host", default=DEFAULT_BACKEND_HOST, help=f"后端地址, 默认: {DEFAULT_BACKEND_HOST}")
    parser.add_argument("--milvus-vectorize", action="store_true", help="导入后触发向量化")
    parser.add_argument("--batch-size", type=int, default=BATCH_SIZE, help=f"批量大小, 默认: {BATCH_SIZE}")
    parser.add_argument("--dry-run", action="store_true", help="仅验证文件不实际导入")
    return parser.parse_args()

def create_es_index(es_host: str, index: str) -> bool:
    """创建或重置 ES 索引"""
    mapping = {
        "settings": {"number_of_shards": 2, "number_of_replicas": 0},
        "mappings": {
            "properties": {
                "case_id": {"type": "keyword"},
                "case_no": {"type": "keyword"},
                "court": {"type": "keyword"},
                "case_type": {"type": "keyword"},
                "judge": {"type": "keyword"},
                "title": {"type": "text", "analyzer": "ik_max_word"},
                "plaintiff": {"type": "keyword"},
                "defendant": {"type": "keyword"},
                "facts": {"type": "text", "analyzer": "ik_max_word"},
                "judgment": {"type": "text", "analyzer": "ik_max_word"},
                "ruling": {"type": "text", "analyzer": "ik_max_word"},
                "legal_basis": {"type": "text", "analyzer": "ik_max_word"},
                "judgment_date": {"type": "date", "format": "yyyy-MM-dd"},
                "tags": {"type": "keyword"},
                "imported_at": {"type": "date"}
            }
        }
    }

    try:
        req = Request(f"{es_host}/{index}", method="DELETE")
        urlopen(req)
        print(f"已删除旧索引: {index}")
    except Exception:
        pass

    try:
        req = Request(f"{es_host}/{index}",
            data=json.dumps(mapping).encode("utf-8"),
            headers={"Content-Type": "application/json"},
            method="PUT")
        resp = urlopen(req)
        print(f"ES 索引创建成功: {index} (HTTP {resp.status})")
        return True
    except Exception as e:
        print(f"ES 索引创建失败: {e}")
        return False

def load_documents(filepath: str) -> List[Dict]:
    """加载文件中的文书数据"""
    docs = []
    if filepath.endswith(".json"):
        with open(filepath, "r", encoding="utf-8") as f:
            data = json.load(f)
            if isinstance(data, list):
                docs = data
            elif isinstance(data, dict) and "data" in data:
                docs = data["data"]
    elif filepath.endswith(".csv"):
        with open(filepath, "r", encoding="utf-8") as f:
            reader = csv.DictReader(f)
            docs = list(reader)
    else:
        print(f"不支持的文件格式: {filepath}")
        sys.exit(1)
    return docs

def normalize_document(raw: Dict, idx: int) -> Dict:
    """标准化文书字段"""
    doc = {
        "case_id": raw.get("case_id", raw.get("id", f"CASE-{idx:06d}")),
        "case_no": raw.get("case_no", raw.get("caseNo", "")),
        "court": raw.get("court", "未知法院"),
        "case_type": raw.get("case_type", raw.get("caseType", "民事")),
        "judge": raw.get("judge", ""),
        "title": raw.get("title", raw.get("caseTitle", "")),
        "plaintiff": raw.get("plaintiff", ""),
        "defendant": raw.get("defendant", ""),
        "facts": raw.get("facts", raw.get("caseFacts", "")),
        "judgment": raw.get("judgment", raw.get("judgmentResult", "")),
        "ruling": raw.get("ruling", raw.get("courtView", "")),
        "legal_basis": raw.get("legal_basis", raw.get("legalBasis", "")),
        "judgment_date": raw.get("judgment_date", raw.get("judgmentDate", time.strftime("%Y-%m-%d"))),
        "tags": raw.get("tags", []),
        "imported_at": time.strftime("%Y-%m-%dT%H:%M:%S")
    }
    if isinstance(doc["tags"], str):
        doc["tags"] = [t.strip() for t in doc["tags"].split(",") if t.strip()]
    return doc

def bulk_import(es_host: str, index: str, docs: List[Dict], batch_size: int) -> int:
    """批量导入 ES"""
    imported = 0
    for i in range(0, len(docs), batch_size):
        batch = docs[i:i + batch_size]
        lines = []
        for doc in batch:
            case_id = doc["case_id"]
            lines.append(json.dumps({"index": {"_index": index, "_id": case_id}}, ensure_ascii=False))
            lines.append(json.dumps(doc, ensure_ascii=False))

        body = "\n".join(lines) + "\n"
        try:
            req = Request(f"{es_host}/_bulk",
                data=body.encode("utf-8"),
                headers={"Content-Type": "application/json"},
                method="POST")
            resp = urlopen(req)
            result = json.loads(resp.read())
            if result.get("errors"):
                for item in result.get("items", []):
                    if "error" in item.get("index", {}):
                        print(f"  导入错误: {item['index']['_id']}: {item['index']['error'].get('reason', 'unknown')}")
            else:
                imported += len(batch)
                print(f"  已导入 {imported}/{len(docs)} ...", end="\r")
        except Exception as e:
            print(f"\n批量导入失败 (批次 {i//batch_size}): {e}")

    print()
    return imported

def trigger_vectorization(backend_host: str, doc_ids: List[str]) -> bool:
    """触发 Milvus 向量化"""
    try:
        req = Request(f"{backend_host}/api/v1/admin/data/import-judgments",
            data=json.dumps({"doc_ids": doc_ids, "action": "vectorize"}).encode("utf-8"),
            headers={"Content-Type": "application/json"},
            method="POST")
        resp = urlopen(req)
        result = json.loads(resp.read())
        print(f"向量化触发: {result}")
        return True
    except Exception as e:
        print(f"向量化触发失败: {e}")
        return False

def build_sample_docs(args) -> List[Dict]:
    """生成示例裁判文书数据（用于测试）"""
    return [
        {
            "case_id": "CASE-2021-001",
            "case_no": "(2021)京01民终1234号",
            "court": "北京市第一中级人民法院",
            "case_type": "民事",
            "judge": "张三",
            "title": "某投资公司与张某合同纠纷案",
            "plaintiff": "某投资公司",
            "defendant": "张某",
            "facts": "张某于2020年3月与某投资公司签订投资协议，约定由投资公司向张某提供创业资金100万元。后张某发现投资公司在签约时隐瞒了公司财务状况，存在欺诈行为，遂诉至法院。",
            "judgment": "一审法院判决撤销投资协议，投资公司返还投资款并赔偿损失。",
            "ruling": "法院认定被告在签订投资协议时存在欺诈行为，根据民法典第148条，判决撤销合同并赔偿损失。",
            "legal_basis": "《中华人民共和国民法典》第一百四十八条、第一百四十九条",
            "judgment_date": "2021-06-15",
            "tags": ["合同纠纷", "欺诈", "投资", "撤销合同"]
        },
        {
            "case_id": "CASE-2022-001",
            "case_no": "(2022)京02民终5678号",
            "court": "北京市第二中级人民法院",
            "case_type": "民事",
            "judge": "李四",
            "title": "李某与北京某公司劳动争议案",
            "plaintiff": "李某",
            "defendant": "北京某科技有限公司",
            "facts": "李某于2019年入职北京某科技公司，担任软件工程师。2021年公司以业务调整为由单方面解除劳动合同，未支付经济补偿金。",
            "judgment": "法院判决公司支付经济补偿金15万元及未休年假工资。",
            "ruling": "公司违法解除劳动合同，根据劳动合同法第46条、第47条，判决支付经济补偿金。",
            "legal_basis": "《中华人民共和国劳动合同法》第四十六条、第四十七条",
            "judgment_date": "2022-03-22",
            "tags": ["劳动争议", "劳动合同", "经济补偿", "违法解除"]
        },
        {
            "case_id": "CASE-2022-002",
            "case_no": "(2022)沪73民终9001号",
            "court": "上海知识产权法院",
            "case_type": "民事",
            "judge": "王五",
            "title": "某软件公司诉某网络公司侵害计算机软件著作权纠纷案",
            "plaintiff": "某软件开发有限公司",
            "defendant": "某网络科技有限公司",
            "facts": "原告开发了一套企业办公管理软件并取得著作权登记。被告未经许可复制、发行该软件，原告发现后诉至法院。",
            "judgment": "法院判决被告停止侵权、赔偿经济损失80万元。",
            "ruling": "被告行为构成著作权侵权，判令停止侵害并赔偿损失。",
            "legal_basis": "《中华人民共和国著作权法》第四十九条、第五十四条",
            "judgment_date": "2022-08-10",
            "tags": ["知识产权", "著作权", "软件侵权", "损害赔偿"]
        },
        {
            "case_id": "CASE-2023-001",
            "case_no": "(2023)粤03民初4567号",
            "court": "深圳市中级人民法院",
            "case_type": "民事",
            "judge": "赵六",
            "title": "某建筑公司诉某地产公司建设工程施工合同纠纷案",
            "plaintiff": "某建筑工程有限公司",
            "defendant": "某房地产开发有限公司",
            "facts": "双方于2020年签订建设工程施工合同，约定由原告承建被告的住宅楼项目。工程竣工后，被告拖欠工程款3600万元未付。",
            "judgment": "法院判决被告支付工程款3600万元及利息。",
            "ruling": "合同合法有效，被告应承担违约责任。",
            "legal_basis": "《中华人民共和国民法典》第八百零七条、最高人民法院建设工程司法解释",
            "judgment_date": "2023-02-28",
            "tags": ["建设工程", "施工合同", "工程款", "违约"]
        },
        {
            "case_id": "CASE-2023-002",
            "case_no": "(2023)浙01民终3210号",
            "court": "杭州市中级人民法院",
            "case_type": "民事",
            "judge": "钱七",
            "title": "王某诉某电商平台网络购物合同纠纷案",
            "plaintiff": "王某",
            "defendant": "某电子商务有限公司",
            "facts": "王某在某电商平台购买商品后，发现商品与描述严重不符，涉嫌虚假宣传。平台拒绝退款并删除差评。",
            "judgment": "法院判决平台退款并三倍赔偿。",
            "ruling": "根据消费者权益保护法第55条，认定平台存在欺诈行为。",
            "legal_basis": "《中华人民共和国消费者权益保护法》第五十五条",
            "judgment_date": "2023-05-12",
            "tags": ["消费者权益", "网络购物", "欺诈", "三倍赔偿"]
        }
    ]

def main():
    args = parse_args()

    if args.dry_run:
        docs = load_documents(args.file) if os.path.exists(args.file) else build_sample_docs(args)
        print(f"DRY RUN: 共 {len(docs)} 条文书记录")
        for i, doc in enumerate(docs[:3]):
            norm = normalize_document(doc, i)
            print(f"  [{i+1}] {norm['case_no']} - {norm.get('title', '')}")
        return

    docs = load_documents(args.file) if os.path.exists(args.file) else build_sample_docs(args)
    normalized = [normalize_document(d, i) for i, d in enumerate(docs)]
    print(f"加载文书: {len(normalized)} 条")

    if not create_es_index(args.es_host, args.es_index):
        print("ES 索引创建失败，终止导入")
        sys.exit(1)

    time.sleep(1)

    imported = bulk_import(args.es_host, args.es_index, normalized, args.batch_size)
    print(f"\n导入完成: {imported}/{len(normalized)} 条记录写入 ES")

    if args.milvus_vectorize and imported > 0:
        doc_ids = [d["case_id"] for d in normalized]
        trigger_vectorization(args.backend_host, doc_ids)

if __name__ == "__main__":
    main()
