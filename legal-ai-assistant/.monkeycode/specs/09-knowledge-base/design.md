# 案例法规库模块技术设计规格

Feature Name: 09-knowledge-base
Updated: 2026-06-16 (基于完整详细方案 v1.0)

## Description

案例法规库模块提供知识库管理功能，支持文档上传、语义分块、团队共享、Milvus向量索引。

---

## 1. 技术架构

```
文件上传 → 文件校验 → 文本提取 → 语义分块 → 向量化 → 存储检索
```

---

## 2. 语义分块策略

| 参数 | 值 | 说明 |
|------|-----|------|
| 分块方式 | 自适应分层 | 按法律条款结构切分 + 长度限制 |
| 块大小 | 512 tokens | 每个Chunk上限 |
| 块重叠 | 64 tokens | 保持语义连续性 |
| 页码标注 | ✅ | 记录每块起始页码 |

---

## 3. API接口设计

### 3.1 知识库列表接口

**GET** `/api/v1/knowledge-base/list`

**Response：**

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "kb_id": "KB-2023-001",
      "name": "建设工程法规库",
      "owner_id": 10001,
      "is_shared": true,
      "document_count": 15,
      "created_at": "2026-06-01T10:00:00+08:00"
    }
  ]
}
```

### 3.2 创建知识库接口

**POST** `/api/v1/knowledge-base/create`

**Request：**

```json
{
  "name": "建设工程法规库",
  "description": "收录建设工程相关法律法规"
}
```

### 3.3 上传文档接口

**POST** `/api/v1/knowledge-base/upload`

**Request：** multipart/form-data

| 字段 | 类型 | 说明 |
|------|------|------|
| file | File | 上传的文件 |
| kb_id | String | 知识库ID |

**Response：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "doc_id": "DOC-2023-001",
    "file_name": "建设工程施工合同司法解释.pdf",
    "file_size": 2048576,
    "status": "processing",
    "chunks_count": 128,
    "created_at": "2026-06-12T10:00:00+08:00"
  }
}
```

### 3.4 删除文档接口

**DELETE** `/api/v1/knowledge-base/{kbId}/documents/{docId}`

---

## 4. 数据模型

### 4.1 MySQL表结构

**knowledge_base表：**

```sql
CREATE TABLE knowledge_base (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    kb_id           VARCHAR(36) NOT NULL UNIQUE,
    name            VARCHAR(100),
    description     TEXT,
    owner_id        BIGINT,
    is_shared       BOOLEAN DEFAULT FALSE,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**kb_document表：**

```sql
CREATE TABLE kb_document (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    doc_id          VARCHAR(36) NOT NULL UNIQUE,
    kb_id           VARCHAR(36),
    file_name       VARCHAR(255),
    file_size       BIGINT,
    status          VARCHAR(20),  -- processing/completed/failed
    chunks_count    INT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_kb (kb_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 4.2 Milvus Collection Schema

```json
{
  "collection": "kb_embeddings",
  "fields": [
    {"name": "chunkId", "type": "VARCHAR", "max_length": 36, "is_primary": true},
    {"name": "docId", "type": "VARCHAR", "max_length": 36},
    {"name": "kbId", "type": "VARCHAR", "max_length": 36},
    {"name": "content", "type": "VARCHAR", "max_length": 2000},
    {"name": "pageNum", "type": "INT"},
    {"name": "embedding", "type": "FLOAT_VECTOR", "dim": 1536}
  ],
  "indexes": [
    {"field": "embedding", "index_type": "HNSW", "metric_type": "COSINE", "params": {"M": 16, "efConstruction": 256}}
  ]
}
```

---

## 5. 错误码定义

| 错误码 | 说明 |
|--------|------|
| KB_001 | 知识库不存在 |
| KB_002 | 无权限 |
| KB_003 | 文件格式不支持 |
| KB_004 | 文件过大 |
| KB_005 | 上传失败 |