# Legal MCP Server

MCP server for Legal AI Assistant backend APIs. Exposes 22 legal AI tools as MCP (Model Context Protocol) tools, covering legal search, case retrieval, company investigation, contract review, document drafting, legal research, and more.

## Tools (22 total)

### Legal Search & Retrieval
| Tool | Description |
|------|-------------|
| `legal_search` | 法规混合检索 - ES全文+Milvus向量混合检索，返回相关度排序的法规条文 |
| `law_search` | 法规查询 - 按名称、类别、效力状态筛选法规库 |
| `get_law_detail` | 获取法规详情 - UUID获取法规全部信息及条文 |
| `law_analyze` | 法规智能分析 - AI解读法规要点、适用场景、关联法规 |

### Case Search & Analysis
| Tool | Description |
|------|-------------|
| `case_search` | 案例查询 - 多条件检索：案件类型、法院级别、判决年份等 |
| `case_similar` | 类案推荐 - 根据案件事实描述查找相似已判决案例 |
| `case_detail` | 获取案例详情 - UUID获取案例完整信息 |
| `case_analyze` | AI案情分析 - 案情摘要、争议焦点、适用法律、判决理由 |

### Company Investigation
| Tool | Description |
|------|-------------|
| `company_query` | 企业查询 - 工商信息、司法风险、经营状况 |

### Contract Review
| Tool | Description |
|------|-------------|
| `contract_review` | AI合同审查 - 全文审查，识别风险条款及修改建议 |
| `contract_dimensions` | 获取审查维度 - 所有风险维度及权重说明 |
| `contract_history` | 审查历史 - 当前用户的合同审查记录列表 |
| `contract_risk_detail` | 获取审查详情 - 完整审查结果含各维度评分 |

### Document Drafting
| Tool | Description |
|------|-------------|
| `document_templates` | 获取文书模板列表 - 所有可用模板及分类 |
| `document_template_detail` | 获取模板详情 - 特定模板的字段说明 |
| `document_draft` | 起草法律文书 - 根据模板和案件信息生成文书内容 |

### Legal Research
| Tool | Description |
|------|-------------|
| `legal_research` | 法律研究（同步）- 结构化研究报告含法律依据、案例参考、风险提示 |
| `legal_research_stream` | 法律研究（流式）- 实时看到检索法规、生成报告各阶段进度 |

### Document Q&A
| Tool | Description |
|------|-------------|
| `doc_qa_ask` | 文档问答 - 基于知识库文档进行智能问答，支持多轮对话 |
| `doc_qa_sessions` | 获取会话列表 - 用户所有问答会话 |
| `doc_qa_create_session` | 创建问答会话 - 新建会话用于连续对话 |
| `doc_qa_session_history` | 获取会话历史 - 指定会话的完整问答记录 |

### Infrastructure
| Tool | Description |
|------|-------------|
| `health_check` | 健康检查 - 后端服务及数据库、Redis连接状态 |
| `ai_status` | AI服务状态 - MiniMax模型可用性检查 |

## Usage

### Run with uv (recommended for local development)

```bash
uv run --directory legal-mcp legal-mcp
```

### Run with uvx (installed package)

```bash
LEGAL_API_BASE_URL=http://localhost:3001 uvx legal-mcp
```

### Configuration

| Environment Variable | Default | Description |
|---------------------|---------|-------------|
| `LEGAL_API_BASE_URL` | `http://localhost:3001` | Backend API base URL |

## OpenCode Integration

Add to `~/.config/opencode/opencode.json`:

```json
{
  "mcp": {
    "servers": {
      "legal-mcp": {
        "type": "local",
        "command": ["uv", "run", "--directory", "/path/to/legal-ai-assistant/legal-mcp", "legal-mcp"],
        "enabled": true,
        "env": {
          "LEGAL_API_BASE_URL": "http://localhost:3001"
        }
      }
    }
  }
}
```

## Requirements

- Python 3.10+
- Legal AI Assistant backend running at the target URL

## Architecture

```
MCP Client (OpenCode)
    |
    | stdio (JSON-RPC)
    v
legal-mcp (Python)
    |
    | HTTP REST
    v
legal-ai-assistant Backend (Spring Boot :3001)
                              |
                              v
                    MySQL / Redis / Elasticsearch / Milvus
```
