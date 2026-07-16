# Legal MCP Server

MCP server for Legal AI Assistant backend APIs. Exposes legal search, company query, contract review, and other AI-powered legal tools as MCP (Model Context Protocol) tools.

## Tools

| Tool | Description |
|------|-------------|
| `legal_search` | 法规混合检索 - ES全文+向量混合检索 |
| `case_similar` | 类案推荐 - 根据案件描述查找相似已判决案例 |
| `case_search` | 案例查询 - 多条件筛选已判决案例 |
| `company_query` | 企业查询 - 工商信息、司法风险、经营状况 |
| `contract_review` | AI合同审查 - 识别风险条款并提供修改建议 |
| `law_search` | 法规查询 - 法律法规数据库多条件筛选 |
| `get_law_detail` | 获取法规详情及全部条文 |
| `doc_qa_ask` | 文档问答 - 基于知识库的智能问答 |

## Usage

### Run with uvx (recommended)

```bash
uvx legal-mcp
```

### Install locally

```bash
pip install -e /path/to/legal-mcp
legal-mcp
```

### Configuration

Set the backend URL via environment variable:

```bash
LEGAL_API_BASE_URL=http://localhost:3001 uvx legal-mcp
```

Default: `http://localhost:3001`

## OpenCode Integration

Add to `~/.config/opencode/opencode.json`:

```json
{
  "mcp": {
    "legal-mcp": {
      "type": "local",
      "command": ["uvx", "legal-mcp"],
      "enabled": true,
      "env": {
        "LEGAL_API_BASE_URL": "http://localhost:3001"
      }
    }
  }
}
```

Or for local development:

```json
{
  "mcp": {
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
```

## Requirements

- Python 3.10+
- Legal AI Assistant backend running at the target URL
- Backend API endpoints (configured in backend `application.yml`):
  - `POST /api/v1/legal-search/search`
  - `POST /api/v1/case-similar/search`
  - `POST /api/v1/case-search/search`
  - `POST /api/v1/company/query`
  - `POST /api/v1/contract/review`
  - `POST /api/v1/law-search/search`
  - `GET /api/v1/law-search/laws/{uuid}`
  - `POST /api/v1/doc-qa/ask`
