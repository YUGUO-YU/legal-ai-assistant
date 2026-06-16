# 案例查询模块技术设计规格

Feature Name: 06-case-search
Updated: 2026-06-16 (基于完整详细方案 v1.0)

## Description

案例查询模块提供司法判例检索功能，支持多维过滤、关键词搜索、分页浏览、脱敏处理。

---

## 1. 技术架构

```
[用户输入搜索条件]
         │
         ▼
[ES查询] → 全文检索 + 过滤条件
         │
         ▼
[结果处理] → 分页 + 排序 + 脱敏
         │
         ▼
[返回结果] → 案例列表 + 分页信息
```

---

## 2. API接口设计

### 2.1 案例搜索接口

**POST** `/api/v1/case-search/search`

**Request：**

```json
{
  "query": "合同纠纷 建设工程",
  "page": 1,
  "pageSize": 10,
  "filters": {
    "case_type": [1],
    "case_cause": ["建设工程合同纠纷"],
    "court_level": [3, 4],
    "trial_procedure": [2],
    "judgment_result": [1, 2],
    "judge_year_min": 2020,
    "judge_year_max": 2024
  },
  "sort": "judge_date",
  "order": "desc"
}
```

**Response：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 1256,
    "page": 1,
    "pageSize": 10,
    "took_ms": 45,
    "items": [
      {
        "case_id": 12345,
        "case_no": "(2023)沪01民终4567号",
        "case_name": "某建筑公司与某地产公司建设工程合同纠纷案",
        "case_type": "民事",
        "case_cause": "建设工程合同纠纷",
        "court_level": 3,
        "court_name": "上海市第一中级人民法院",
        "judge_date": "2023-08-15",
        "judgment_result": 2,
        "litigation_amount": 5000000,
        "summary": "法院认定被告构成违约，判决支付工程款...",
        "source_url": "https://wenshu.court.gov.cn/...",
        "source_name": "中国裁判文书网"
      }
    ]
  }
}
```

### 2.2 案例详情接口

**GET** `/api/v1/case-search/{caseId}`

---

## 3. 过滤条件设计

| 字段 | 类型 | 支持多选 | 说明 |
|------|------|---------|------|
| case_type | int[] | 是 | 案件类型：刑事/民事/行政/执行/赔偿 |
| case_cause | string[] | 是 | 案由编码（精确匹配） |
| court_level | int[] | 是 | 法院层级：最高院/高院/中院/基层 |
| trial_procedure | int[] | 是 | 审理程序：一审/二审/再审/复核 |
| judgment_result | int[] | 是 | 裁判结果：全部支持/部分支持/驳回/撤诉/调解 |
| judge_year_min/max | int | 否 | 裁判年份范围 |

### 3.1 案件类型编码

| 编码 | 类型 |
|------|------|
| 1 | 民事 |
| 2 | 刑事 |
| 3 | 行政 |
| 4 | 执行 |
| 5 | 赔偿 |

### 3.2 裁判结果编码

| 编码 | 结果 |
|------|------|
| 1 | 全部支持 |
| 2 | 部分支持 |
| 3 | 驳回 |
| 4 | 撤诉 |
| 5 | 调解 |

---

## 4. 分页查询优化

**Search After游标分页：**

```json
{
  "pagination": {
    "mode": "search_after",
    "search_after": [0.95, "2021-09-15", 12345678]
  }
}
```

---

## 5. 错误码定义

| 错误码 | 说明 |
|--------|------|
| CS_001 | 案例不存在 |
| CS_002 | 分页错误 |
| CS_003 | 查询超时 |