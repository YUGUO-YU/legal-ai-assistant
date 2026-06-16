# 企业查询模块技术设计规格

Feature Name: 05-company-query
Updated: 2026-06-16 (基于完整详细方案 v1.0)

## Description

查询企业工商信息、股东结构、司法风险等。

**数据来源：**

| 数据源 | 优先级 | 覆盖内容 |
|--------|--------|----------|
| 信用中国 | 1（优先） | 工商信息、行政处罚、失信 |
| 企查查 | 2 | 全维度（工商、司法、IP、风险） |
| 天眼查 | 3 | 辅助交叉验证 |

---

## 1. 技术架构

```
[用户输入企业名称/统一社会信用代码]
         │
         ▼
[数据源查询] → 企查查API + 天眼查API + 信用中国
         │
         ▼
[数据聚合] → 去重 + 交叉验证
         │
         ▼
[风险评估] → 三级风险预警
         │
         ▼
[结果返回] → 企业信息 + 股东结构 + 风险预警
```

---

## 2. API接口设计

### 2.1 企业查询接口

**POST** `/api/v1/company/query`

**Request：**

```json
{
  "companyName": "阿里巴巴（中国）有限公司",
  "creditCode": "91330000xxxxxxx"
}
```

**Response：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "companyName": "阿里巴巴（中国）有限公司",
    "creditCode": "91330000xxxxxxx",
    "legalPerson": "张勇",
    "registeredCapital": 50000000,
    "establishDate": "1999-09-09",
    "businessStatus": "在业",
    "shareholders": [
      {
        "name": "阿里巴巴集团控股有限公司",
        "shares": 100,
        "amount": 50000000,
        "type": "企业法人"
      }
    ],
    "risks": [
      {
        "type": "被执行人",
        "level": "LOW",
        "count": 2,
        "totalAmount": 50000
      }
    ],
    "riskLevel": "LOW",
    "source": "企查查 | 查询时间：2026-06-12 10:40:02"
  }
}
```

### 2.2 风险等级接口

**GET** `/api/v1/company/risk-levels`

**Response：**

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {"level": "HIGH", "label": "高风险", "color": "red"},
    {"level": "MEDIUM", "label": "中风险", "color": "yellow"},
    {"level": "LOW", "label": "低风险", "color": "blue"}
  ]
}
```

---

## 3. 风险预警机制

### 3.1 预警级别定义

| 级别 | 颜色 | 响应时效 | 触发条件 |
|------|------|----------|----------|
| HIGH | 🔴 红色 | 立即通知 | 失信被执行/严重违法/近1年行政处罚 |
| MEDIUM | 🟡 黄色 | 24小时内 | 历史行政处罚>3条/被执行记录>5条 |
| LOW | 🔵 蓝色 | 定期汇总 | 经营异常（已移出）/少量历史处罚 |

### 3.2 风险类型

| 风险类型 | 说明 |
|----------|------|
| 被执行人 | 有未结执行案件 |
| 失信被执行人 | 已被列入失信名单 |
| 行政处罚 | 受到工商/税务等行政处罚 |
| 经营异常 | 经营地址异常等 |

---

## 4. 数据模型

### CompanyInfoResponse

```java
public class CompanyInfoResponse {
    String companyName;
    String creditCode;
    String legalPerson;
    BigDecimal registeredCapital;
    LocalDate establishDate;
    String businessStatus;
    List<Shareholder> shareholders;
    List<JudicialRisk> risks;
    String riskLevel;
    String source;
}

public class Shareholder {
    String name;
    BigDecimal shares;
    BigDecimal amount;
    String type;  // 企业法人/自然人
}

public class JudicialRisk {
    String type;
    String level;
    Integer count;
    BigDecimal totalAmount;
}
```

---

## 5. 错误码定义

| 错误码 | 说明 |
|--------|------|
| COMP_001 | 企业不存在 |
| COMP_002 | 查询超时 |
| COMP_003 | 限流 |
| COMP_004 | 数据源不可用 |