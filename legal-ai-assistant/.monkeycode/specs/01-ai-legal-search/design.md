# AI搜法技术设计规格

Feature Name: 01-ai-legal-search
Updated: 2026-06-16 (基于完整详细方案 v1.0)

## Description

AI搜法模块通过自然语言提问，从法规数据库中检索匹配条文，并给出带来源标注的解释。

**核心能力：**
- ES全文检索 + Milvus向量检索混合搜索
- 溯源到具体条款URL，真实性100%
- 自然语言理解，降低检索门槛
- 支持多法条关联与上下文理解

---

## 1. 技术架构

### 1.1 系统架构

```
[用户] → [Vue3前端] → [API Gateway] → [SearchService]
                                          │
                    ┌─────────────────────┼─────────────────────┐
                    ▼                     ▼                     ▼
              [Elasticsearch]        [MySQL]              [Redis]
              (全文+向量双索引)       (法规元数据)           (热点缓存)
                    │
                    ▼
              [MiniMax-M3 AI]
              (OpenAI 兼容) → 生成回答 + 引用溯源
```

### 1.2 数据流向（9步）

```
[1] Query理解 → 实体抽取(法律术语) + 同义扩展
[2] 意图识别 → 分类：条文检索/法律问答/法条关联
[3] 混合检索 → ES BM25(关键词) + Milvus ANN(向量)
[4] 结果初筛 → 召回Top-50，去重
[5] 重排序(Rerank) → 交叉编码重排，输出Top-10
[6] 生成回答 → MiniMax-M3 组装修正
[7] 引用溯源 → 标注来源URL
[8] 追问建议 → 生成3个追问推荐
[9] 结果返回 → 前端渲染
```

---

## 2. 数据模型

### 2.1 MySQL表结构（6张表）

**law_document（法规主表）：**

```sql
CREATE TABLE law_document (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    law_uuid        VARCHAR(64) NOT NULL UNIQUE COMMENT '法规UUID',
    title           VARCHAR(500) NOT NULL COMMENT '法规标题',
    short_title     VARCHAR(200) COMMENT '法规简称',
    category_l1     VARCHAR(50) NOT NULL COMMENT '一级分类：法律/行政法规/部门规章/地方性法规',
    category_l2     VARCHAR(100) COMMENT '二级分类',
    issuing_authority VARCHAR(200) COMMENT '发布机关',
    issue_date      DATE COMMENT '发布日期',
    effective_date  DATE COMMENT '生效日期',
    status          TINYINT DEFAULT 1 COMMENT '状态：1现行 2废止 3修订中',
    source_url      VARCHAR(500) COMMENT '来源URL',
    source_name     VARCHAR(100) COMMENT '来源名称',
    view_count      INT DEFAULT 0 COMMENT '浏览次数',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category (category_l1, category_l2),
    INDEX idx_status (status),
    INDEX idx_issue_date (issue_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**law_article（法规条款表）：**

```sql
CREATE TABLE law_article (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    law_id          BIGINT NOT NULL COMMENT '关联法规ID',
    article_uuid    VARCHAR(64) NOT NULL UNIQUE,
    article_no      VARCHAR(50) NOT NULL COMMENT '条款编号：如"第148条"',
    title           VARCHAR(200) COMMENT '条款标题',
    content         TEXT NOT NULL COMMENT '条款正文',
    content_hash    VARCHAR(64) COMMENT '内容哈希（去重）',
    sort_order      INT DEFAULT 0,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (law_id) REFERENCES law_document(id) ON DELETE CASCADE,
    INDEX idx_law_id (law_id),
    INDEX idx_article_no (article_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**legal_case（判例表）：**

```sql
CREATE TABLE legal_case (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    case_uuid       VARCHAR(64) NOT NULL UNIQUE,
    case_no        VARCHAR(64) COMMENT '案号',
    title          VARCHAR(500) NOT NULL,
    court          VARCHAR(128) COMMENT '法院名称',
    case_type      VARCHAR(32) COMMENT '案件类型：民事/刑事/行政',
    case_cause     VARCHAR(128) COMMENT '案由',
    judgment_date  DATE COMMENT '裁判日期',
    summary        TEXT COMMENT '裁判摘要',
    full_text_url  VARCHAR(500) COMMENT '全文URL',
    source_url     VARCHAR(500) COMMENT '来源URL',
    source_name    VARCHAR(100) COMMENT '来源名称：裁判文书网/北大法宝',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_case_type (case_type),
    INDEX idx_court (court),
    INDEX idx_judgment_date (judgment_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**search_log（搜索日志）：**

```sql
CREATE TABLE search_log (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         VARCHAR(64) COMMENT '用户ID（可为空）',
    query_text      VARCHAR(500) NOT NULL,
    intent_type     VARCHAR(32) COMMENT '意图类型',
    result_count    INT DEFAULT 0,
    response_time_ms INT DEFAULT 0,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_query (query_text(50)),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**search_feedback（搜索反馈）：**

```sql
CREATE TABLE search_feedback (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    search_log_id   BIGINT NOT NULL,
    article_id      BIGINT COMMENT '反馈的条款ID',
    is_helpful      TINYINT COMMENT '是否有用：1有用 0无用',
    user_comment    TEXT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (search_log_id) REFERENCES search_log(id),
    FOREIGN KEY (article_id) REFERENCES law_article(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**law_relation（法规关联表）：**

```sql
CREATE TABLE law_relation (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    source_article_id BIGINT NOT NULL COMMENT '来源条款',
    target_article_id BIGINT NOT NULL COMMENT '目标条款',
    relation_type   VARCHAR(20) COMMENT '引用关系：cites/amends/replaces',
    weight          DECIMAL(5,2) DEFAULT 1.0,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_relation (source_article_id, target_article_id),
    FOREIGN KEY (source_article_id) REFERENCES law_article(id),
    FOREIGN KEY (target_article_id) REFERENCES law_article(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 2.2 Milvus Collection设计

```python
# Collection: legal_law_articles
{
  "fields": [
    {"name": "article_id", "type": "VARCHAR", "max_length": 64, "is_primary": True},
    {"name": "law_id", "type": "INT64"},
    {"name": "content_vector", "type": "FLOAT_VECTOR", "dim": 1536},  # text-embedding-3-small
    {"name": "article_no", "type": "VARCHAR", "max_length": 50},
    {"name": "created_at", "type": "INT64"}
  ],
  "indexes": [
    {"field": "content_vector", "index_type": "HNSW", "metric_type": "COSINE", "params": {"M": 16, "efConstruction": 256}}
  ]
}
```

### 2.3 Elasticsearch索引配置

```json
PUT /legal_law_articles
{
  "mappings": {
    "properties": {
      "article_id": {"type": "keyword"},
      "law_id": {"type": "long"},
      "title": {"type": "text", "analyzer": "ik_smart", "fields": {"keyword": {"type": "keyword"}}},
      "article_no": {"type": "keyword"},
      "content": {"type": "text", "analyzer": "ik_smart"},
      "category_l1": {"type": "keyword"},
      "category_l2": {"type": "keyword"},
      "issuing_authority": {"type": "keyword"},
      "effective_date": {"type": "date"},
      "status": {"type": "integer"},
      "source_url": {"type": "keyword"}
    }
  }
}
```

---

## 3. API接口设计

### 3.1 法规检索接口

**POST** `/api/v1/legal-search/search`

**Request：**

```json
{
  "query": "合同欺诈如何认定？民法典相关规定",
  "page": 1,
  "pageSize": 10,
  "filters": {
    "category_l1": ["法律"],
    "status": [1],
    "effective_date_range": {
      "start": "2020-01-01",
      "end": "2026-12-31"
    }
  },
  "include_cases": true,
  "highlight": true
}
```

**Response：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 128,
    "page": 1,
    "pageSize": 10,
    "took_ms": 45,
    "items": [
      {
        "article_id": "ART-2023-001",
        "law_id": "LAW-2023-001",
        "law_title": "中华人民共和国民法典",
        "article_no": "第一百四十八条",
        "title": "欺诈的认定",
        "content": "一方以欺诈手段，使对方在违背真实意思的情况下订立的合同，受欺诈方有权请求人民法院或者仲裁机构予以撤销。",
        "highlights": ["一方以<em>欺诈</em>手段，使对方在违背真实意思的情况下订立的合同"],
        "source_url": "https://flk.npc.gov.cn/detail2.html?...",
        "source_name": "国家法律法规信息库",
        "score": 18.56,
        "related_cases_count": 5
      }
    ],
    "related_cases": [
      {
        "case_uuid": "CASE-2021-12345",
        "case_no": "(2021)沪01民终1234号",
        "title": "某投资公司与张某合同纠纷案",
        "court": "上海市第一中级人民法院",
        "summary": "法院认定被告在签订投资协议时存在欺诈行为，判决撤销合同。",
        "source_url": "https://wenshu.court.gov.cn/...",
        "source_name": "中国裁判文书网"
      }
    ]
  }
}
```

### 3.2 条款详情接口

**GET** `/api/v1/legal-search/articles/{articleId}`

**Response：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "article_id": "ART-2023-001",
    "law_title": "中华人民共和国民法典",
    "article_no": "第一百四十八条",
    "title": "欺诈的认定",
    "content": "一方以欺诈手段，使对方在违背真实意思的情况下订立的合同，受欺诈方有权请求人民法院或者仲裁机构予以撤销。",
    "legal_basis": [
      {"type": "条文", "content": "《民法典》第一百四十九条：第三人实施欺诈行为，使一方陷入错误认识的，适用欺诈规定。"}
    ],
    "related_articles": [
      {"article_id": "ART-2023-002", "article_no": "第一百四十九条", "relation": "引用"}
    ],
    "source_url": "https://flk.npc.gov.cn/...",
    "source_name": "国家法律法规信息库"
  }
}
```

### 3.3 搜索反馈接口

**POST** `/api/v1/legal-search/feedback`

**Request：**

```json
{
  "search_log_id": 12345,
  "article_id": "ART-2023-001",
  "is_helpful": 1,
  "user_comment": "回答准确，法条引用正确"
}
```

---

## 4. 提示词设计

### 4.1 System Prompt

```markdown
# 角色定义

你是一个专业的法律助手，专注于中国法律法规的检索与解读。你拥有法学背景，能够准确理解法律条文含义并给出专业解释。

# 核心任务

根据用户输入的法律问题，从检索到的法规条文中提取相关信息，给出准确、专业的回答。

# 约束条件（HARD RULES）

1. **溯源必须**：每个法律结论必须标注来源，格式为：[法规名称] 第X条 | 来源URL
2. **禁止胡编**：只陈述检索结果中明确存在的内容，不得编造、推测法条内容
3. **不确定声明**：如检索结果不足以回答，明确说明"未检索到相关内容"
4. **语言严谨**：使用规范法律用语，避免口语化表达
5. **时效性**：注意标注法条的时效性，提示可能已修订

# 输出格式

## 回答
[正文内容]

## 参考依据
1. [法规名称] 第X条 | 来源URL
2. [法规名称] 第X条 | 来源URL

## 追问建议
- 问题1
- 问题2
- 问题3

---

⚠️ 免责声明：本回答基于检索到的法律法规生成，仅供参考，不构成法律意见。
```

---

## 5. Components and Interfaces

### LegalSearchController

| Method | Path | Description |
|--------|------|-------------|
| POST | /api/v1/legal-search/search | 执行混合检索 |
| GET | /api/v1/legal-search/articles/{articleId} | 获取法规详情 |
| POST | /api/v1/legal-search/feedback | 提交检索反馈 |

### LegalSearchService

```java
public LegalSearchResponse search(LegalSearchRequest request)
// 执行混合检索：ES + Milvus → RRF融合 → 返回结果

public SearchResultItem getArticleDetail(String articleId)
// 获取单条法规详情

public Void submitFeedback(FeedbackRequest request)
// 提交用户反馈
```

---

## 6. 边界情况处理

| 场景 | 处理策略 |
|------|----------|
| 检索结果为空 | 返回空结果页面，提示"未找到相关法规，建议更换关键词" |
| 法规已废止/修订 | 显示当前有效版本，标注"该法规已于XXX废止，现行有效版本为..." |
| 来源URL失效 | 标注"[来源可能已更新]"而非显示死链 |
| 超时（ES查询>2s） | 降级为缓存结果 + 提示"检索耗时较长，结果仅供参考" |
| 用户未登录 | 允许搜索，但结果不记录用户行为（privacy设计） |
| 关键词涉敏 | 触发内容安全过滤，返回"该查询不符合服务范围" |

---

## 7. 真实性保障机制

### 7.1 来源URL白名单

| 来源名称 | URL模式 | 可信度 |
|----------|---------|--------|
| 国家法律法规信息库 | `flk.npc.gov.cn` | ⭐⭐⭐⭐⭐ |
| 全国人大官网 | `npc.gov.cn` | ⭐⭐⭐⭐⭐ |
| 最高人民法院 | `court.gov.cn` | ⭐⭐⭐⭐⭐ |
| 中国裁判文书网 | `wenshu.court.gov.cn` | ⭐⭐⭐⭐ |
| 北大法宝 | `pkulaw.cn` | ⭐⭐⭐⭐ |

### 7.2 AI幻觉检测伪代码

```java
public class HallucinationDetector {
    public boolean isHallucinated(String claim, List<Citation> citations) {
        // 1. 检查claim中的法条引用是否存在于citations
        for (String articleRef : extractArticleRefs(claim)) {
            if (!citationsContain(articleRef, citations)) {
                return true;  // 引用了不存在的法条
            }
        }
        // 2. 检查claim是否超出citations覆盖范围
        if (claimContainsUnsupportedFact(claim, citations)) {
            return true;  // 陈述了citations不支持的事实
        }
        return false;
    }
}
```

---

## 8. 错误码定义

| 错误码 | 说明 |
|--------|------|
| LAW_001 | 法规不存在 |
| LAW_002 | 参数错误 |
| LAW_003 | 检索超时 |
| LAW_004 | 来源URL失效 |

---

## 9. Configuration

```yaml
legal-search:
  elasticsearch:
    enabled: true
    index: "legal_law_articles"
    shards: 3
    replicas: 1
  milvus:
    enabled: true
    collection: "legal_law_articles"
    dimension: 1536
    top-k: 50
  hybrid:
    es-weight: 0.4
    vector-weight: 0.6
    rrf-k: 60
  embedding:
    provider: "minimax"
    model: "embo-01"
```