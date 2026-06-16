# AI文书起草模块技术设计规格

Feature Name: 03-ai-document
Updated: 2026-06-16 (基于完整详细方案 v1.0)

## Description

根据用户选择的文书类型和案件信息，智能生成法律文书。

**支持的20种文书类型：**

| 分类 | 文书类型 | 数量 |
|------|----------|------|
| 民事诉讼 | 民事起诉状、答辩状、上诉状、财产保全申请书、强制执行申请书 | 5 |
| 劳动人事 | 劳动合同、保密协议、竞业限制协议、解除劳动合同协议、劳动仲裁申请书 | 5 |
| 商业函件 | 律师函、CEO函、合同解除通知函、催款函、法律意见书请求函 | 5 |
| 知识产权 | 商标许可合同、软件许可协议、商业秘密保密协议 | 3 |
| 民商事通用 | 授权委托书 | 1 |

**核心价值：**
- 平均起草时间从2-4小时缩短至5-10分钟
- 格式符合最新法律规范
- 自动识别法律风险点
- 强制包含免责声明

---

## 1. 支持的20种文书模板

### 1.1 民事诉讼类（5种）

| 模板代码 | 模板名称 | 引用法律 |
|---------|---------|---------|
| civil_petition | 民事起诉状 | 民诉法第122条 |
| civil_defense | 民事答辩状 | 民诉法第128条 |
| civil_appeal | 民事上诉状 | 民诉法第170条 |
| civil_property保全 | 财产保全申请书 | 民诉法第100条 |
| civil_execution | 强制执行申请书 | 民诉法第236条 |

### 1.2 劳动人事类（5种）

| 模板代码 | 模板名称 | 引用法律 |
|---------|---------|---------|
| labor_contract | 劳动合同 | 劳动合同法第10条 |
| labor_confidentiality | 保密协议 | 劳动合同法第23条 |
| labor_non_compete | 竞业限制协议 | 劳动合同法第23条 |
| labor_termination | 解除劳动合同协议 | 劳动合同法第37条 |
| labor_arbitration | 劳动仲裁申请书 | 劳动争议调解仲裁法第28条 |

### 1.3 商业函件类（5种）

| 模板代码 | 模板名称 | 引用法律 |
|---------|---------|---------|
| business_lawyer_letter | 律师函 | 民法典第577条 |
| business_ceo_letter | CEO函 | 民法典第577条 |
| business_contract_termination | 合同解除通知函 | 民法典第565条 |
| business_payment_demand | 催款函 | 民法典第676条 |
| business_legal_opinion_request | 法律意见书请求函 | 民法典第535条 |

### 1.4 知识产权类（3种）

| 模板代码 | 模板名称 | 引用法律 |
|---------|---------|---------|
| ip_trademark_license | 商标许可合同 | 商标法第40条 |
| ip_software_license | 软件许可协议 | 著作权法第26条 |
| ip_confidentiality | 商业秘密保密协议 | 反不正当竞争法第9条 |

### 1.5 民商事通用类（1种）

| 模板代码 | 模板名称 | 引用法律 |
|---------|---------|---------|
| common_power_of_attorney | 授权委托书 | 民诉法第59条 |

---

## 2. API接口设计

### 2.1 文书起草接口

**POST** `/api/v1/document/draft`

**Request：**

```json
{
  "templateCode": "civil_petition",
  "caseData": {
    "plaintiffName": "李四",
    "plaintiffAddress": "北京市朝阳区",
    "defendantName": "王五",
    "defendantAddress": "上海市浦东新区",
    "claimAmount": 100000,
    "claimDescription": "借款合同纠纷",
    "courtName": "北京市朝阳区"
  },
  "language": "zh-CN",
  "enableAI": true
}
```

**Response：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "documentContent": "[民事起诉状正文...]",
    "riskPrompt": "【风险提示】\n1. 涉及金额较大，建议委托专业律师代理。\n2. 诉讼时效：民事案件的诉讼时效为三年。\n3. 证据保全：建议保留好相关合同、付款凭证...",
    "disclaimer": "本法律文书由AI辅助生成，仅供参考。使用前请务必由具有执业资格的律师进行审核和修改。",
    "referencedLaws": ["《中华人民共和国民事诉讼法》第一百二十二条"],
    "documentId": "DOC-2023-001",
    "generatedAt": 1718160000
  }
}
```

### 2.2 模板列表接口

**GET** `/api/v1/document/templates`

**Response：**

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "templateCode": "civil_petition",
      "templateName": "民事起诉状",
      "category": "民事诉讼",
      "popular": true
    }
  ]
}
```

---

## 3. 提示词设计

### 3.1 System Prompt（民事起诉状）

```markdown
# 角色定义

你是一位拥有10年以上民事诉讼经验的中国执业律师，擅长起草各类民事法律文书。

# 核心任务

根据用户提供的案件信息，起草符合中国法院受理标准的民事起诉状。

# 约束条件

1. 法律依据准确性：引用的法条必须是现行有效法律（以《民法典》为主）
2. 格式严格规范：遵循最高人民法院发布的民事起诉状书写规范
3. 语言专业严谨：使用规范法律用语，避免口语化
4. 内容完整无遗漏：必须包含当事人信息、诉讼请求、事实与理由、证据清单
5. 金额计算准确：利息计算需注明LPR标准和起止日期
6. 风险提示：以【风险提示】标注形式指出文书中的潜在风险点

# 输出格式

[民事起诉状正文]
---
【风险提示】
[识别到的风险点和修改建议]
---
【免责声明】
本民事起诉状由AI辅助生成，仅供参考。使用前请务必由具有执业资格的律师进行审核和修改。
```

### 3.2 免责声明生成逻辑

```java
public String generateDisclaimer(String templateCode, BigDecimal amount) {
    String base = "本法律文书由AI辅助生成，仅供参考。";
    
    if (amount.compareTo(new BigDecimal("50000")) > 0) {
        // 金额>5万，增强级免责声明
        return base + "鉴于本文书涉及金额较大，强烈建议您咨询专业律师。";
    }
    
    // 涉及人身权益的文书类型
    Set<String> personalTypes = Set.of("divorce", "inheritance", "labor_dispute");
    if (personalTypes.contains(templateCode)) {
        return base + "鉴于本文书涉及人身权益，强烈建议您咨询专业律师。";
    }
    
    return base;
}
```

---

## 4. 安全控制

### 4.1 人工复核触发条件

**强制复核（不可跳过）：**

| 条件 | 阈值 |
|------|------|
| 诉讼标的金额 | > 20,000元 |
| 合同标的金额 | > 500,000元 |
| 涉及人身权益 | 离婚/继承/劳动争议 |
| 风险等级 | AI判定为高风险 |

---

## 5. 错误码定义

| 错误码 | 说明 |
|--------|------|
| DRAFT_001 | 模板不存在 |
| DRAFT_002 | 生成失败 |
| DRAFT_003 | 需人工复核 |
| DRAFT_004 | 参数错误 |