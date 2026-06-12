# 法律AI助手系统 API 文档

## 概述

法律AI助手系统后端API，基于Spring Boot 3.2开发，采用Mock数据模式，无需外部依赖即可运行。

**基础URL**: `http://localhost:3001/api/v1`

---

## 认证接口

### 登录

```
POST /auth/login
```

**请求体**:
```json
{
  "username": "demo",
  "password": "demo123"
}
```

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "mock_token_xxx",
    "expireTime": 1234567890000,
    "userInfo": {
      "userId": 1,
      "username": "demo",
      "nickname": "法律用户",
      "email": "demo@example.com",
      "role": "lawyer"
    }
  }
}
```

### 获取用户信息

```
GET /auth/user-info
Authorization: Bearer {token}
```

### 登出

```
POST /auth/logout
Authorization: Bearer {token}
```

---

## 法律检索

### AI法律检索

```
POST /legal-search/search
```

**请求体**:
```json
{
  "query": "合同欺诈如何认定",
  "page": 1,
  "pageSize": 10,
  "includeCases": true
}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "total": 3,
    "page": 1,
    "pageSize": 10,
    "tookMs": 45,
    "items": [
      {
        "articleId": "ART-2023-001",
        "lawId": "LAW-2023-001",
        "lawTitle": "中华人民共和国民法典",
        "articleNo": "第一百四十八条",
        "title": "欺诈的认定",
        "content": "一方以欺诈手段...",
        "highlights": ["<em>合同</em>"],
        "sourceUrl": "https://...",
        "sourceName": "国家法律法规信息库",
        "score": 18.56,
        "relatedCasesCount": 2
      }
    ],
    "relatedCases": [...]
  }
}
```

### 法规查询

```
POST /law-search/search
GET /law-search/categories
```

---

## 案例检索

### 类案搜索

```
POST /case-similar/search
```

**请求体**:
```json
{
  "caseDescription": "房屋买卖合同纠纷，卖方违约拒绝交房",
  "caseType": 1,
  "caseCause": "合同纠纷"
}
```

### 案例查询

```
POST /case-search/search
```

**请求体**:
```json
{
  "keyword": "合同纠纷",
  "caseType": [1, 2],
  "courtLevel": [3, 4],
  "trialProcedure": 1,
  "judgmentResult": 1,
  "page": 1,
  "pageSize": 10
}
```

---

## 文档服务

### 文书起草

```
POST /document/draft
GET /document/templates
GET /document/templates/{code}
```

### 文档问答

```
POST /doc-qa/ask
```

**请求体**:
```json
{
  "question": "合同欺诈的构成要件是什么？",
  "sessionId": null
}
```

---

## 企业服务

### 企业查询

```
POST /company/query
```

**请求体**:
```json
{
  "companyName": "测试公司"
}
```

---

## 合同审查

### 合同审查

```
POST /contract/review
GET /contract/dimensions
```

**请求体**:
```json
{
  "contractText": "买卖合同：甲方出售房屋给乙方...",
  "contractType": "sale",
  "contractAmount": 1000000
}
```

---

## 知识库

### 知识库管理

```
GET  /knowledge-base/list
POST /knowledge-base/create
DELETE /knowledge-base/{id}
POST /knowledge-base/upload
```

---

## 健康检查

```
GET /health
```

**响应**:
```json
{
  "service": "legal-ai-assistant",
  "version": "1.0.0",
  "status": "UP"
}
```

---

## 错误码

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未授权 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |
