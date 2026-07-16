"""
Legal AI Assistant MCP Server.

Wraps the legal-ai-assistant backend REST APIs as MCP tools for use
in OpenCode and other MCP-compatible clients.

Usage:
    uvx legal-mcp

Or install locally:
    pip install -e .
    python -m legal_mcp
"""

import os
import httpx
from typing import Any
from mcp.server import Server
from mcp.server.stdio import stdio_server
from mcp.types import Tool, TextContent

# Backend API configuration
API_BASE_URL = os.environ.get("LEGAL_API_BASE_URL", "http://localhost:3001")
API_TIMEOUT = 60.0

# Initialize HTTP client
http_client = httpx.AsyncClient(timeout=API_TIMEOUT, base_url=API_BASE_URL)

# Server instance
server = Server("legal-ai-assistant")


def _make_request(method: str, path: str, data: dict | None = None, params: dict | None = None) -> dict[str, Any]:
    """Make synchronous HTTP request (used via asyncio)."""
    import httpx
    client = httpx.Client(timeout=API_TIMEOUT, base_url=API_BASE_URL)
    try:
        if method.upper() == "GET":
            resp = client.get(path, params=params)
        else:
            resp = client.post(path, json=data)
        resp.raise_for_status()
        result = resp.json()
        # Handle ApiResponse wrapper: {code: 200, data: {...}}
        if isinstance(result, dict):
            if result.get("code") == 200:
                return result.get("data", result)
            elif "data" in result:
                return result["data"]
        return result
    finally:
        client.close()


async def _async_request(method: str, path: str, data: dict | None = None, params: dict | None = None) -> dict[str, Any]:
    """Make async HTTP request to backend."""
    try:
        if method.upper() == "GET":
            resp = await http_client.get(path, params=params)
        else:
            resp = await http_client.post(path, json=data)
        resp.raise_for_status()
        result = resp.json()
        # Handle ApiResponse wrapper: {code: 200, data: {...}}
        if isinstance(result, dict):
            if result.get("code") == 200:
                return result.get("data", result)
            elif "data" in result:
                return result["data"]
        return result
    except httpx.HTTPStatusError as e:
        return {"error": f"HTTP {e.response.status_code}: {e.response.text}"}
    except httpx.ConnectError:
        return {"error": f"Cannot connect to backend at {API_BASE_URL}. Is the server running?"}
    except Exception as e:
        return {"error": str(e)}


# =============================================================================
# Tool Definitions
# =============================================================================

TOOLS = [
    Tool(
        name="legal_search",
        description="法规混合检索 - 支持ES全文检索和Milvus向量检索的混合检索。输入关键词查询相关法律法规条文。适用于查找特定法规条款、了解法律条文内容。",
        inputSchema={
            "type": "object",
            "properties": {
                "query": {"type": "string", "description": "检索关键词，如'劳动合同解除'、'房屋租赁纠纷'"},
                "page": {"type": "integer", "description": "页码，默认1", "default": 1},
                "pageSize": {"type": "integer", "description": "每页条数，默认10", "default": 10},
                "includeCases": {"type": "boolean", "description": "是否包含相关案例", "default": False}
            },
            "required": ["query"]
        }
    ),
    Tool(
        name="case_similar",
        description="类案推荐 - 根据案件描述查找相似的已判决案例。输入案件事实描述，返回相似案例列表及相似度。适用于了解类似案件判决结果。",
        inputSchema={
            "type": "object",
            "properties": {
                "description": {"type": "string", "description": "案件事实描述，包含当事人情况、纠纷经过等"},
                "caseType": {"type": "string", "description": "案件类型，如'民事'、'刑事'、'行政'", "default": ""},
                "limit": {"type": "integer", "description": "返回数量，默认5", "default": 5}
            },
            "required": ["description"]
        }
    ),
    Tool(
        name="case_search",
        description="案例查询 - 根据条件检索已判决案例，支持按案件类型、审理法院、判决年份等筛选。",
        inputSchema={
            "type": "object",
            "properties": {
                "keyword": {"type": "string", "description": "检索关键词"},
                "caseType": {"type": "array", "items": {"type": "string"}, "description": "案件类型列表，如['民事','刑事']", "default": []},
                "courtLevel": {"type": "array", "items": {"type": "string"}, "description": "法院级别列表", "default": []},
                "trialProcedure": {"type": "string", "description": "审理程序"},
                "judgmentResult": {"type": "string", "description": "判决结果关键词"},
                "judgeYearMin": {"type": "integer", "description": "判决年份最小值"},
                "judgeYearMax": {"type": "integer", "description": "判决年份最大值"},
                "page": {"type": "integer", "description": "页码", "default": 1},
                "pageSize": {"type": "integer", "description": "每页条数", "default": 10}
            }
        }
    ),
    Tool(
        name="company_query",
        description="企业查询 - 根据企业名称或统一社会信用代码查询企业工商信息、司法风险、经营状况等。适用于企业背景调查、风险评估。",
        inputSchema={
            "type": "object",
            "properties": {
                "companyName": {"type": "string", "description": "企业名称（全称或简称）"},
                "unifiedSocialCreditCode": {"type": "string", "description": "统一社会信用代码（18位）"},
                "enableRiskWarning": {"type": "boolean", "description": "是否启用风险预警", "default": True}
            }
        }
    ),
    Tool(
        name="contract_review",
        description="AI合同审查 - 对合同文本进行AI智能审查，识别风险条款、不合理条款并提供修改建议。适用于合同签订前的风险把控。",
        inputSchema={
            "type": "object",
            "properties": {
                "text": {"type": "string", "description": "合同文本内容（支持粘贴完整合同）"},
                "templateCode": {"type": "string", "description": "合同类型模板代码，如'劳动法'、'房屋买卖'，留空则自动识别", "default": ""},
                "dimensions": {"type": "array", "items": {"type": "string"}, "description": "审查维度列表", "default": []}
            },
            "required": ["text"]
        }
    ),
    Tool(
        name="law_search",
        description="法规查询 - 查询法律法规数据库，支持按名称、类别、状态筛选，返回法规基本信息及效力状态。",
        inputSchema={
            "type": "object",
            "properties": {
                "keyword": {"type": "string", "description": "法规名称关键词"},
                "status": {"type": "integer", "description": "效力状态：1=现行有效 2=已废止 3=修订中 4=尚未生效 5=部分失效", "default": None},
                "page": {"type": "integer", "description": "页码", "default": 1},
                "pageSize": {"type": "integer", "description": "每页条数", "default": 10}
            }
        }
    ),
    Tool(
        name="get_law_detail",
        description="获取法规详情 - 根据法规UUID获取法规详细信息及全部条文内容。",
        inputSchema={
            "type": "object",
            "properties": {
                "lawUuid": {"type": "string", "description": "法规UUID（从law_search结果中获取）"}
            },
            "required": ["lawUuid"]
        }
    ),
    Tool(
        name="doc_qa_ask",
        description="文档问答 - 基于上传的文档或知识库内容进行问答。可用于分析法律条文、解释合同条款、回答具体法律问题。",
        inputSchema={
            "type": "object",
            "properties": {
                "question": {"type": "string", "description": "用户问题"},
                "kbId": {"type": "string", "description": "知识库ID（可选，不填则使用默认知识库）", "default": ""},
                "sessionId": {"type": "string", "description": "会话ID（用于连续对话上下文）", "default": ""}
            },
            "required": ["question"]
        }
    ),
]


# =============================================================================
# Tool Handlers
# =============================================================================

@server.list_tools()
async def list_tools() -> list[Tool]:
    """Return all available MCP tools."""
    return TOOLS


@server.call_tool()
async def call_tool(name: str, arguments: dict[str, Any]) -> list[TextContent]:
    """Handle tool calls from MCP clients."""
    result = await _dispatch(name, arguments)

    if isinstance(result, dict) and "error" in result:
        return [TextContent(type="text", text=f"Error: {result['error']}")]

    # Format result as readable text
    text = _format_result(name, result)
    return [TextContent(type="text", text=text)]


async def _dispatch(name: str, arguments: dict[str, Any]) -> dict[str, Any]:
    """Dispatch request to appropriate backend API."""
    if name == "legal_search":
        return await _async_request("POST", "/api/v1/legal-search/search", {
            "query": arguments["query"],
            "page": arguments.get("page", 1),
            "pageSize": arguments.get("pageSize", 10),
            "includeCases": arguments.get("includeCases", False)
        })

    elif name == "case_similar":
        return await _async_request("POST", "/api/v1/case-similar/search", {
            "description": arguments["description"],
            "caseType": arguments.get("caseType", ""),
            "limit": arguments.get("limit", 5)
        })

    elif name == "case_search":
        return await _async_request("POST", "/api/v1/case-search/search", {
            "keyword": arguments.get("keyword", ""),
            "caseType": arguments.get("caseType", []),
            "courtLevel": arguments.get("courtLevel", []),
            "trialProcedure": arguments.get("trialProcedure"),
            "judgmentResult": arguments.get("judgmentResult"),
            "judgeYearMin": arguments.get("judgeYearMin"),
            "judgeYearMax": arguments.get("judgeYearMax"),
            "page": arguments.get("page", 1),
            "pageSize": arguments.get("pageSize", 10)
        })

    elif name == "company_query":
        return await _async_request("POST", "/api/v1/company/query", {
            "companyName": arguments.get("companyName"),
            "unifiedSocialCreditCode": arguments.get("unifiedSocialCreditCode"),
            "enableRiskWarning": arguments.get("enableRiskWarning", True)
        })

    elif name == "contract_review":
        return await _async_request("POST", "/api/v1/contract/review", {
            "text": arguments["text"],
            "templateCode": arguments.get("templateCode", ""),
            "dimensions": arguments.get("dimensions", [])
        })

    elif name == "law_search":
        return await _async_request("POST", "/api/v1/law-search/search", {
            "keyword": arguments.get("keyword", ""),
            "status": arguments.get("status"),
            "page": arguments.get("page", 1),
            "pageSize": arguments.get("pageSize", 10)
        })

    elif name == "get_law_detail":
        return await _async_request("GET", f"/api/v1/law-search/laws/{arguments['lawUuid']}")

    elif name == "doc_qa_ask":
        return await _async_request("POST", "/api/v1/doc-qa/ask", {
            "question": arguments["question"],
            "kbId": arguments.get("kbId", ""),
            "sessionId": arguments.get("sessionId", "")
        })

    else:
        return {"error": f"Unknown tool: {name}"}


def _format_result(name: str, result: Any) -> str:
    """Format API result as readable text for the user."""
    if not result:
        return "未找到相关结果"

    if name == "legal_search":
        items = result.get("items", [])
        total = result.get("total", 0)
        if not items:
            return "未找到相关法规"
        lines = [f"找到 {total} 条相关法规：\n"]
        for item in items[:10]:
            law_title = item.get("title", item.get("lawTitle", ""))
            content = item.get("content", item.get("snippet", ""))[:200]
            relevance = item.get("relevanceScore", item.get("score", 0))
            lines.append(f"【{law_title}】(相关度:{relevance:.2f})")
            lines.append(f"  {content}...")
            lines.append("")
        return "\n".join(lines)

    elif name == "case_similar":
        items = result if isinstance(result, list) else result.get("items", [])
        if not items:
            return "未找到相似案例"
        lines = ["找到相似案例：\n"]
        for item in items[:5]:
            title = item.get("caseName", item.get("title", ""))
            court = item.get("court", "")
            date = item.get("judgmentDate", item.get("date", ""))
            result_text = item.get("judgmentResult", item.get("result", ""))[:100]
            similarity = item.get("similarity", item.get("score", 0))
            lines.append(f"【{title}】(相似度:{similarity:.1%})")
            lines.append(f"  法院：{court} | 判决日期：{date}")
            lines.append(f"  判决结果：{result_text}")
            lines.append("")
        return "\n".join(lines)

    elif name == "case_search":
        items = result.get("items", [])
        total = result.get("total", 0)
        if not items:
            return "未找到案例"
        lines = [f"找到 {total} 条案例：\n"]
        for item in items[:10]:
            title = item.get("caseName", item.get("title", ""))
            court = item.get("court", "")
            case_type = item.get("caseType", "")
            result_text = item.get("judgmentResult", "")[:100]
            lines.append(f"【{title}】{case_type}")
            lines.append(f"  {court}")
            lines.append(f"  {result_text}")
            lines.append("")
        return "\n".join(lines)

    elif name == "company_query":
        if isinstance(result, dict) and result.get("companyName"):
            name_val = result.get("companyName", "")
            legal_person = result.get("legalPerson", result.get("legal_person", ""))
            reg_capital = result.get("registeredCapital", result.get("reg_capital", ""))
            status = result.get("businessStatus", result.get("status", ""))
            risk_level = result.get("riskLevel", "")
            lines = [f"企业信息：{name_val}"]
            if legal_person:
                lines.append(f"  法定代表人：{legal_person}")
            if reg_capital:
                lines.append(f"  注册资本：{reg_capital}")
            if status:
                lines.append(f"  经营状态：{status}")
            if risk_level:
                lines.append(f"  风险等级：{risk_level}")
            # Key risks
            risks = result.get("risks", result.get("keyRisks", {}))
            if isinstance(risks, dict):
                for k, v in risks.items():
                    if v:
                        lines.append(f"  {k}：{v}")
            return "\n".join(lines)
        return str(result)

    elif name == "contract_review":
        if isinstance(result, dict):
            overall = result.get("overallRisk", result.get("riskLevel", ""))
            dimensions = result.get("dimensions", [])
            risks = result.get("risks", [])
            lines = [f"合同审查结果 - 总体风险：{overall}\n"]
            if dimensions:
                lines.append("各维度评分：")
                for dim in dimensions if isinstance(dimensions, list) else []:
                    name_d = dim.get("name", "")
                    score = dim.get("score", 0)
                    lines.append(f"  {name_d}：{score}/10")
            if risks and isinstance(risks, list):
                lines.append("\n风险条款：")
                for r in risks[:5]:
                    clause = r.get("clause", r.get("description", ""))[:100]
                    suggestion = r.get("suggestion", r.get("recommendation", ""))[:100]
                    lines.append(f"  - {clause}")
                    if suggestion:
                        lines.append(f"    建议：{suggestion}")
            return "\n".join(lines)
        return str(result)

    elif name == "law_search":
        items = result if isinstance(result, list) else result.get("items", result.get("laws", []))
        total = result.get("total", 0) if isinstance(result, dict) else 0
        if not items:
            return "未找到法规"
        lines = [f"找到 {total} 条法规：\n"]
        for item in items[:10]:
            title = item.get("title", item.get("lawTitle", ""))
            status_map = {1: "现行有效", 2: "已废止", 3: "修订中", 4: "尚未生效", 5: "部分失效"}
            status = item.get("status", 1)
            status_text = status_map.get(status, status)
            issue_date = item.get("issueDate", item.get("issue_date", ""))
            lines.append(f"【{title}】{status_text} | 发布日期：{issue_date}")
        return "\n".join(lines)

    elif name == "get_law_detail":
        if isinstance(result, dict):
            title = result.get("title", result.get("lawTitle", ""))
            status = result.get("status", 1)
            status_map = {1: "现行有效", 2: "已废止", 3: "修订中", 4: "尚未生效", 5: "部分失效"}
            issuing = result.get("issuingAuthority", result.get("issuing_authority", ""))
            articles = result.get("articles", [])
            lines = [f"法规名称：{title}\n"]
            lines.append(f"效力状态：{status_map.get(status, status)}")
            if issuing:
                lines.append(f"发布机关：{issuing}")
            lines.append(f"\n共 {len(articles)} 条条文：\n")
            for art in (articles[:20] if articles else []):
                art_no = art.get("articleNumber", art.get("article_no", ""))
                content = art.get("content", "")[:200]
                lines.append(f"第{art_no}条：{content}")
            return "\n".join(lines)
        return str(result)

    elif name == "doc_qa_ask":
        if isinstance(result, dict):
            answer = result.get("answer", result.get("response", ""))
            sources = result.get("sources", result.get("chunks", []))
            lines = [f"AI回答：\n{answer}\n"]
            if sources:
                lines.append(f"\n参考来源（共{len(sources)}条）：")
                for src in sources[:3]:
                    text = src.get("text", src.get("content", ""))[:100]
                    source = src.get("source", src.get("file", ""))
                    lines.append(f"  - [{source}] {text}...")
            return "\n".join(lines)
        return str(result)

    else:
        return str(result)


# =============================================================================
# Server Entry Point
# =============================================================================

mcp = server


async def main():
    """Run the MCP server."""
    async with stdio_server() as (read_stream, write_stream):
        await server.run(
            read_stream,
            write_stream,
            server.create_initialization_options()
        )


if __name__ == "__main__":
    import asyncio
    asyncio.run(main())
