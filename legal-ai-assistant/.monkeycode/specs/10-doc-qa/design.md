# AI文件问答模块技术设计规格

Feature Name: 10-ai-doc-qa
Updated: 2026-06-16 (基于完整详细方案 v1.0)

## Description

AI文件问答模块基于RAG检索，支持多轮对话和引用溯源。

---

## 1. 技术架构

```
用户Query → 向量化 → Milvus ANN → Top-50
                    ↓
              MySQL FULLTEXT → 关键词匹配
                    ↓
              RRF融合排序（k=60）
                    ↓
              知识库权重0.7 / 互联网权重0.3
                    ↓
              Top-5相关片段 → 注入Prompt
```

---

## 2. API接口设计

### 2.1 问答接口

**POST** `/api/v1/doc-qa/ask`

**Request：**

```json
{
  "query": "这份合同中的违约金条款是否过高？",
  "kb_id": "KB-2023-001",
  "session_id": "SESSION-2023-001",
  "top_k": 5
}
```

**Response：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "answer": "根据《民法典》第五百八十五条规定，违约金过高的标准是超过实际损失的30%。您合同中约定的违约金为合同金额的20%，在法定标准范围内。",
    "citations": [
      {
        "doc_id": "DOC-2023-001",
        "doc_name": "民法典.pdf",
        "chunk_id": "CHUNK-001",
        "content": "当事人约定的违约金超过造成损失的百分之三十的，视为过高...",
        "page_num": 156
      }
    ],
    "session_id": "SESSION-2023-001",
    "created_at": "2026-06-12T10:00:00+08:00"
  }
}
```

### 2.2 会话历史接口

**GET** `/api/v1/doc-qa/history`

**Query参数：**
| 参数 | 类型 | 说明 |
|------|------|------|
| session_id | String | 会话ID |
| page | int | 页码 |
| page_size | int | 每页大小 |

**Response：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "session_id": "SESSION-2023-001",
    "messages": [
      {
        "role": "user",
        "content": "这份合同中的违约金条款是否过高？",
        "created_at": "2026-06-12T10:00:00+08:00"
      },
      {
        "role": "assistant",
        "content": "根据《民法典》第五百八十五条规定...",
        "citations": [],
        "created_at": "2026-06-12T10:00:01+08:00"
      }
    ]
  }
}
```

---

## 3. 多轮对话上下文管理

| 层级 | 存储 | TTL |
|------|------|-----|
| 会话元数据 | MySQL + Redis | 30min |
| 对话历史（最近20轮） | Redis（热点）+ MySQL | 30min |
| 检索上下文 | Redis | 10min |
| LLM上下文窗口 | 内存 | 单次请求 |

---

## 4. 引用溯源

### 4.1 Citation结构

```java
public class Citation {
    String docId;        // 文档ID
    String docName;      // 文档名称
    String chunkId;      // 块ID
    String content;      // 引用内容片段
    Integer pageNum;     // 页码
    Double relevance;    // 相关度分数
}
```

### 4.2 前端展示

- 在答案中以超链接形式展示引用来源
- 支持点击跳转到源文档对应位置
- 悬浮显示引用内容预览

---

## 5. 错误码定义

| 错误码 | 说明 |
|--------|------|
| QA_001 | 会话不存在 |
| QA_002 | 文档未解析 |
| QA_003 | 检索超时 |
| QA_004 | 生成失败 |