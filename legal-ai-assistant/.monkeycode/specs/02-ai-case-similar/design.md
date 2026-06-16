# AI类案模块技术设计规格

Feature Name: 02-ai-case-similar
Updated: 2026-06-16 (基于完整详细方案 v1.0)

## Description

输入案件描述，自动匹配相似判例，分析裁判要点，提供类案参考。

**核心价值：**
- 案件要素提取精度 ≥ 85%
- 语义匹配代替关键词匹配
- 裁判要点自动生成
- 类案判决结果可量化统计

---

## 1. 技术架构

```
[用户输入案件描述]
         │
         ▼
[案件要素提取] → 原告/被告/案由/诉求/事实/法律争议点
         │
         ▼
[向量化] → text-embedding-3-small
         │
         ▼
[Milvus ANN检索] → Top-N相似案例
         │
         ▼
[多维度匹配评分] → 案由(30%)+事实(40%)+诉求(20%)+法律问题(10%)
         │
         ▼
[生成类案报告] → 裁判要点 + 引用来源
         │
         ▼
[返回前端] → 类案列表 + 相似度评分 + 裁判结果
```

---

## 2. 数据模型

### 2.1 MySQL表结构（8张表）

**tb_case（案例主表）：**

```sql
CREATE TABLE tb_case (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    case_uuid       VARCHAR(64) NOT NULL UNIQUE,
    case_no         VARCHAR(64) COMMENT '案号',
    case_name       VARCHAR(500) NOT NULL,
    case_type       TINYINT COMMENT '案件类型：1民事 2刑事 3行政',
    case_cause      VARCHAR(128) COMMENT '案由',
    court_level     TINYINT COMMENT '法院层级：1最高院 2高院 3中院 4基层',
    court_name      VARCHAR(128) COMMENT '法院名称',
    judge_date      DATE COMMENT '裁判日期',
    trial_procedure VARCHAR(20) COMMENT '审理程序：一审/二审/再审',
    judgment_result TINYINT COMMENT '裁判结果：1全部支持 2部分支持 3驳回',
    litigation_amount DECIMAL(18,2) COMMENT '诉讼金额',
    plaintiff       VARCHAR(256) COMMENT '原告',
    defendant       VARCHAR(256) COMMENT '被告',
    key_facts       TEXT COMMENT '关键事实',
    judgment_summary TEXT COMMENT '裁判摘要',
    legal_basis     JSON COMMENT '法律依据',
    vector_status   TINYINT DEFAULT 0 COMMENT '向量化状态：0待处理 1已完成',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_case_type (case_type),
    INDEX idx_case_cause (case_cause),
    INDEX idx_court_level (court_level),
    INDEX idx_judge_date (judge_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**tb_case_element（案件要素表）：**

```sql
CREATE TABLE tb_case_element (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    case_id         BIGINT NOT NULL,
    element_type   VARCHAR(32) NOT NULL COMMENT '要素类型：plaintiff/defendant/claim/fact/dispute',
    element_key     VARCHAR(128) COMMENT '要素key',
    element_value   TEXT NOT NULL COMMENT '要素值',
    importance      DECIMAL(3,2) DEFAULT 1.0 COMMENT '重要程度权重',
    FOREIGN KEY (case_id) REFERENCES tb_case(id) ON DELETE CASCADE,
    INDEX idx_case_id (case_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**tb_case_embedding（案例向量表）：**

```sql
CREATE TABLE tb_case_embedding (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    case_id         BIGINT NOT NULL,
    embedding_type  VARCHAR(32) NOT NULL COMMENT '向量化类型：fact/claim/dispute',
    vector_id       VARCHAR(64) COMMENT 'Milvus中的vector_id',
    dimension       INT DEFAULT 1536,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_case_embedding (case_id, embedding_type),
    FOREIGN KEY (case_id) REFERENCES tb_case(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**tb_case_label（案例标签表）：**

```sql
CREATE TABLE tb_case_label (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    case_id         BIGINT NOT NULL,
    label_type     VARCHAR(32) COMMENT '标签类型：case_cause/industry/region',
    label_value    VARCHAR(128) NOT NULL,
    FOREIGN KEY (case_id) REFERENCES tb_case(id) ON DELETE CASCADE,
    INDEX idx_label (label_type, label_value)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**tb_similar_case（类案记录表）：**

```sql
CREATE TABLE tb_similar_case (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    source_case_id  BIGINT NOT NULL COMMENT '源案例ID',
    similar_case_id BIGINT NOT NULL COMMENT '相似案例ID',
    similarity_score DECIMAL(5,4) NOT NULL COMMENT '相似度得分',
    matching_features JSON COMMENT '匹配特征：{fact: 0.8, claim: 0.6, ...}',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_pair (source_case_id, similar_case_id),
    FOREIGN KEY (source_case_id) REFERENCES tb_case(id),
    FOREIGN KEY (similar_case_id) REFERENCES tb_case(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 3. API接口设计

### 3.1 类案检索接口

**POST** `/api/v1/case-similar/search`

**Request：**

```json
{
  "case_description": "2024年3月，我与某装修公司签订装修合同，约定总价15万元。施工过程中，装修公司多次擅自变更材料品牌，且进度严重滞后。现已超过约定竣工日期2个月。我要求解除合同并退还已付款项。",
  "case_type": 1,
  "case_cause": "装饰装修合同纠纷",
  "top_k": 10,
  "filters": {
    "court_level": [3, 4],
    "judgment_result": [1, 2],
    "judge_year_min": 2020
  }
}
```

**Response：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "source_case_hash": "案件描述向量指纹",
    "total_similar": 156,
    "items": [
      {
        "case_id": 12345,
        "case_no": "(2023)沪01民终4567号",
        "case_name": "李某与上海某装饰公司装饰装修合同纠纷案",
        "court_level": 3,
        "court_name": "上海市第一中级人民法院",
        "judge_date": "2023-08-15",
        "judgment_result": 2,
        "litigation_amount": 180000,
        "similarity_score": 0.92,
        "matching_features": {
          "fact_similarity": 0.95,
          "claim_similarity": 0.88,
          "dispute_similarity": 0.90
        },
        "key_facts": "原告与被告签订装修合同，被告擅自变更材料品牌且进度滞后...",
        "judgment_summary": "法院认定被告构成违约，判决解除合同，退还已付款项...",
        "legal_basis": ["《民法典》第577条", "《建设工程施工合同司法解释》第12条"],
        "source_url": "https://wenshu.court.gov.cn/...",
        "source_name": "中国裁判文书网"
      }
    ],
    "statistics": {
      "total_count": 156,
      "win_rate": 0.73,
      "avg_compensation": 156000
    }
  }
}
```

---

## 4. 相似度算法设计

### 4.1 加权公式

```
Similarity = 0.3 × fact_sim + 0.4 × claim_sim + 0.2 × dispute_sim + 0.1 × court_level_match

其中：
- fact_sim：事实要素相似度（基于案件事实向量）
- claim_sim：诉讼请求相似度（基于诉讼请求向量）
- dispute_sim：法律争议点相似度（基于争议焦点向量）
- court_level_match：法院层级匹配（同类法院权重加成）
```

### 4.2 案件要素提取流程

```
Step 1: 文本预处理 → 分句、分词、命名实体识别
Step 2: 实体抽取 → 原告、被告、案由、金额、时间
Step 3: 事实抽取 → 关键事件、因果关系
Step 4: 争议焦点识别 → 分类为合同履行/违约/解除等
Step 5: 向量化 → 各要素独立向量
Step 6: 存储 → Milvus + MySQL
```

### 4.3 Rerank策略

```python
def rerank_candidates(query_vector, candidates, top_n=5):
    """
    交叉编码Rerank：使用cross-encoder对候选案例重新排序
    """
    cross_encoder = CrossEncoder('cross-encoder/ms-marco-MiniLM-L-6-v2')
    
    pairs = [(query, case['description']) for case in candidates]
    scores = cross_encoder.predict(pairs)
    
    # 按cross-encoder得分重新排序
    ranked = sorted(zip(candidates, scores), key=lambda x: x[1], reverse=True)
    return ranked[:top_n]
```

---

## 5. 错误码定义

| 错误码 | 说明 |
|--------|------|
| CASE_001 | 案例不存在 |
| CASE_002 | 要素提取失败 |
| CASE_003 | 相似度计算超时 |