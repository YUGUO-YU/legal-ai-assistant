# AI合同审查模块技术设计规格

Feature Name: 08-ai-contract-review
Updated: 2026-06-16 (基于完整详细方案 v1.0)

## Description

AI合同审查模块提供8维度风险评估，支持三级风险分级和逐项改进建议。

---

## 1. 审查维度（8维度）

| 维度代码 | 维度名称 | 权重 |
|----------|----------|------|
| SUBJECT_QUALIFICATION | 主体资格 | 15% |
| CONTRACT_VALIDITY | 合同效力 | 20% |
| RIGHTS_OBLIGATIONS | 权利义务 | 15% |
| BREACH_RESPONSIBILITY | 违约责任 | 15% |
| DISPUTE_RESOLUTION | 争议解决 | 10% |
| EXEMPTION_CLAUSE | 免责条款 | 10% |
| INTELLECTUAL_PROPERTY | 知识产权 | 8% |
| PERSONAL_INFO | 个人信息 | 7% |

---

## 2. API接口设计

### 2.1 合同审查接口

**POST** `/api/v1/contract/review`

**Request：**

```json
{
  "contractText": "甲方（出租方）：XXX\n乙方（承租方）：YYY\n根据《民法典》相关规定...",
  "contractType": "lease",
  "contractName": "房屋租赁合同"
}
```

**Response：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "contractName": "房屋租赁合同",
    "contractType": "lease",
    "overallRisk": {
      "score": 65,
      "level": "MEDIUM",
      "label": "中风险"
    },
    "dimensions": [
      {
        "code": "SUBJECT_QUALIFICATION",
        "name": "主体资格",
        "score": 80,
        "level": "LOW",
        "issues": [],
        "suggestions": []
      },
      {
        "code": "CONTRACT_VALIDITY",
        "name": "合同效力",
        "score": 45,
        "level": "MEDIUM",
        "issues": [
          "租赁期限超过20年部分可能无效"
        ],
        "suggestions": [
          "建议将租赁期限控制在20年以内，或约定到期自动续约"
        ]
      },
      {
        "code": "RIGHTS_OBLIGATIONS",
        "name": "权利义务",
        "score": 60,
        "level": "MEDIUM",
        "issues": [],
        "suggestions": []
      }
    ],
    "riskDistribution": {
      "HIGH": 0,
      "MEDIUM": 2,
      "LOW": 6
    },
    "reviewedAt": "2026-06-12T10:00:00+08:00"
  }
}
```

### 2.2 审查维度定义接口

**GET** `/api/v1/contract/dimensions`

**Response：**

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {"code": "SUBJECT_QUALIFICATION", "name": "主体资格", "weight": 0.15},
    {"code": "CONTRACT_VALIDITY", "name": "合同效力", "weight": 0.20},
    {"code": "RIGHTS_OBLIGATIONS", "name": "权利义务", "weight": 0.15},
    {"code": "BREACH_RESPONSIBILITY", "name": "违约责任", "weight": 0.15},
    {"code": "DISPUTE_RESOLUTION", "name": "争议解决", "weight": 0.10},
    {"code": "EXEMPTION_CLAUSE", "name": "免责条款", "weight": 0.10},
    {"code": "INTELLECTUAL_PROPERTY", "name": "知识产权", "weight": 0.08},
    {"code": "PERSONAL_INFO", "name": "个人信息", "weight": 0.07}
  ]
}
```

---

## 3. 风险分级判断标准

### 3.1 高风险（3级）

满足任一条件即为高风险：
- 违反法律、行政法规强制性规定
- 可能导致合同无效或被撤销
- 潜在财产损失超过合同金额30%
- 违约金超过法定标准2倍以上

### 3.2 中风险（2级）

- 可能导致一方重大权益受损
- 条款表述模糊导致争议频发
- 违约金超出法定标准1-2倍

### 3.3 低风险（1级）

- 表述不够清晰，建议优化措辞
- 存在潜在隐患但短期内风险可控

---

## 4. 综合风险评分算法

```
RS = Σ(si × wi)
风险评分 = RS / 10（映射到0-100）

风险等级判定：
- ≥ 70分 → 高风险
- 40-69分 → 中风险
- < 40分 → 低风险
```

---

## 5. 错误码定义

| 错误码 | 说明 |
|--------|------|
| REVIEW_001 | 文档解析失败 |
| REVIEW_002 | 审查超时 |
| REVIEW_003 | 合同文本过长 |
| REVIEW_004 | 不支持的合同类型 |