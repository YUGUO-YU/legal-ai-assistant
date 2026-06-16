# 法规查询模块技术设计规格

Feature Name: 07-law-search
Updated: 2026-06-16 (基于完整详细方案 v1.0)

## Description

法规查询模块提供法规分类浏览和版本追溯功能。

---

## 1. 法规分类体系

### 1.1 一级分类

| 分类 | 说明 |
|------|------|
| 法律 | 全国人大及常委会制定 |
| 行政法规 | 国务院制定 |
| 部门规章 | 国务院各部委制定 |
| 地方性法规 | 省级人大及常委会制定 |
| 司法解释 | 最高人民法院/最高人民检察院 |

### 1.2 二级分类

| 一级分类 | 二级分类 |
|----------|----------|
| 法律 | 民法、商法、刑法、行政法、经济法、诉讼法、非诉讼程序法、劳动法、环境资源法、知识产权法 |
| 行政法规 | 国务院组织法、民政类、公安类、司法类、财政类、税务类等 |
| 部门规章 | 国土资源类、环境保护类、城乡建设类等 |
| 地方性法规 | 省/直辖市/自治区法规 |

---

## 2. API接口设计

### 2.1 法规搜索接口

**POST** `/api/v1/law-search/search`

**Request：**

```json
{
  "query": "合同 效力",
  "page": 1,
  "pageSize": 10,
  "filters": {
    "category_l1": ["法律"],
    "category_l2": ["民法"],
    "status": [1],
    "effective_date_range": {
      "start": "2020-01-01",
      "end": "2026-12-31"
    }
  }
}
```

**Response：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 256,
    "page": 1,
    "pageSize": 10,
    "items": [
      {
        "law_id": "LAW-2023-001",
        "law_title": "中华人民共和国民法典",
        "short_title": "民法典",
        "category_l1": "法律",
        "category_l2": "民法",
        "status": 1,
        "status_label": "现行有效",
        "effective_date": "2021-01-01",
        "source_url": "https://flk.npc.gov.cn/...",
        "source_name": "国家法律法规信息库"
      }
    ]
  }
}
```

### 2.2 法规分类接口

**GET** `/api/v1/law-search/categories`

**Response：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "categories": [
      {
        "code": "law",
        "name": "法律",
        "children": [
          {"code": "civil", "name": "民法"},
          {"code": "criminal", "name": "刑法"},
          {"code": "administrative", "name": "行政法"}
        ]
      },
      {
        "code": "regulation",
        "name": "行政法规",
        "children": []
      }
    ]
  }
}
```

### 2.3 法规版本历史接口

**GET** `/api/v1/law-search/{lawId}/versions`

---

## 3. 效力状态管理

| 状态码 | 状态名称 | 显示标识 |
|--------|----------|----------|
| 1 | 现行有效 | 🟢 现行有效 |
| 2 | 已废止 | 🔴 已废止 |
| 3 | 修订中 | 🟡 修订中 |
| 4 | 尚未生效 | ⏳ 尚未生效 |
| 5 | 部分失效 | 🟠 部分失效 |

---

## 4. 关联推荐

当查看某法规详情时，系统应推荐：
- 同主题法规
- 上位法（如有）
- 下位法/配套规定
- 引用该法规的条款

---

## 5. 错误码定义

| 错误码 | 说明 |
|--------|------|
| LS_001 | 法规不存在 |
| LS_002 | 版本错误 |
| LS_003 | 查询超时 |