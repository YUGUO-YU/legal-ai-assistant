"""
Legal AI Assistant MCP Server.

Wraps the legal-ai-assistant backend REST APIs as MCP tools for use
in OpenCode and other MCP-compatible clients.

Usage:
    uv run --directory legal-mcp legal-mcp

Or install locally:
    pip install -e .
    python -m legal_mcp
"""

import os
import re
import httpx
from typing import Any
from mcp.server import Server
from mcp.server.stdio import stdio_server
from mcp.types import Tool, TextContent

API_BASE_URL = os.environ.get("LEGAL_API_BASE_URL", "http://localhost:3001")
API_TIMEOUT = 120.0
MAX_RETRIES = 3
RETRY_BACKOFF = 1.5
SEARCH_API_KEY = os.environ.get("SEARCH_API_KEY", "")
SEARCH_API_URL = os.environ.get("SEARCH_API_URL", "https://ddg-api.herokuapp.com/search")

server = Server("legal-ai-assistant")


def _retry_request(fn):
    """Decorator for retry with exponential backoff."""
    import time
    def wrapper(*args, **kwargs):
        last_err = None
        for attempt in range(MAX_RETRIES):
            try:
                return fn(*args, **kwargs)
            except (httpx.ConnectError, httpx.TimeoutException, httpx.HTTPStatusError) as e:
                last_err = e
                if attempt < MAX_RETRIES - 1:
                    time.sleep(RETRY_BACKOFF ** attempt)
        return {"error": f"Failed after {MAX_RETRIES} retries: {last_err}"}
    return wrapper


@_retry_request
def _make_request(method: str, path: str, data: dict | None = None, params: dict | None = None) -> dict[str, Any]:
    """Make synchronous HTTP request with retry support."""
    client = httpx.Client(timeout=API_TIMEOUT, base_url=API_BASE_URL)
    try:
        if method.upper() == "GET":
            resp = client.get(path, params=params)
        else:
            resp = client.post(path, json=data)
        resp.raise_for_status()
        result = resp.json()
        if isinstance(result, dict):
            if result.get("code") == 200:
                return result.get("data", result)
            elif "data" in result:
                return result["data"]
        return result
    finally:
        client.close()


def _stream_request(method: str, path: str, data: dict | None = None, params: dict | None = None) -> list[str]:
    """Make streaming SSE request, return accumulated text chunks."""
    client = httpx.Client(timeout=API_TIMEOUT, base_url=API_BASE_URL, headers={"Accept": "text/event-stream"})
    try:
        if method.upper() == "GET":
            req = client.build_request("GET", path, params=params)
        else:
            req = client.build_request("POST", path, json=data)
        resp = client.send(req, stream=True)
        resp.raise_for_status()
        chunks = []
        for line in resp.iter_lines():
            if line.startswith("data:"):
                chunks.append(line[5:].strip())
        return chunks
    finally:
        client.close()


def _parse_sse_events(chunks: list[str]) -> dict[str, Any]:
    """Parse SSE data chunks into structured result."""
    result = {"type": "complete", "phases": [], "content": "", "error": None}
    content_parts = []
    for chunk in chunks:
        if not chunk or chunk == "[DONE]":
            continue
        try:
            import json
            event = json.loads(chunk)
            t = event.get("type", "")
            if t == "progress":
                result["phases"].append({
                    "phase": event.get("phase", ""),
                    "progress": event.get("progress", 0),
                    "message": event.get("message", "")
                })
            elif t == "report":
                content_parts.append(event.get("content", ""))
            elif t == "report_complete":
                content_parts.append(event.get("content", ""))
            elif t == "error":
                result["error"] = event.get("message", "Unknown error")
        except (json.JSONDecodeError, Exception):
            if chunk:
                content_parts.append(chunk)
    result["content"] = "".join(content_parts)
    return result


def _search_web(query: str) -> str:
    """Perform real web search with multi-source fallback for maximum data authenticity.

    Strategy:
    1. If SEARCH_API_KEY is configured, use paid search API (SerpAPI etc.)
    2. DuckDuckGo Instant Answer API (fast, structured)
    3. DuckDuckGo HTML scrape (more results, needs parsing)
    4. Google search via SERP proxy if configured
    """
    # --- Strategy 1: Paid API (SerpAPI or compatible) ---
    if SEARCH_API_KEY and SEARCH_API_URL:
        try:
            params = {"q": query, "api_key": SEARCH_API_KEY}
            resp = httpx.get(SEARCH_API_URL, params=params, timeout=30)
            resp.raise_for_status()
            data = resp.json()
            results = _parse_serp_results(data)
            if results:
                return results
        except Exception:
            pass

    # --- Strategy 2: DuckDuckGo Instant Answer (primary) ---
    result = _ddg_instant_answer(query)
    if result and result != f"未找到关于'{query}'的相关信息":
        return result

    # --- Strategy 3: DuckDuckGo HTML (fallback) ---
    result = _ddg_html_search(query)
    if result:
        return result

    return f"未找到关于'{query}'的相关信息"


def _ddg_instant_answer(query: str, timeout: float = 20.0) -> str:
    """DuckDuckGo Instant Answer API - fast, structured results."""
    try:
        url = f"https://api.duckduckgo.com/?q={query}&format=json&no_html=1&skip_disambig=1&hl=zh-cn"
        resp = httpx.get(url, timeout=timeout)
        resp.raise_for_status()
        data = resp.json()
        lines = []
        if data.get("AbstractText"):
            heading = data.get("Heading") or query
            lines.append(f"【{heading}】\n{data['AbstractText']}\n来源: {data.get('AbstractURL', 'https://duckduckgo.com')}\n")
        for topic in data.get("RelatedTopics", [])[:15]:
            if topic.get("Text"):
                lines.append(f"- {topic['Text']}")
        return "\n".join(lines) if lines else f"未找到关于'{query}'的相关信息"
    except Exception:
        return f"未找到关于'{query}'的相关信息"


def _ddg_html_search(query: str, timeout: float = 20.0) -> str:
    """DuckDuckGo HTML page scrape - returns more results than Instant Answer API."""
    try:
        encoded = httpx.utils.encode_url(query, safe="/:")
        url = f"https://html.duckduckgo.com/html/?q={encoded}&kl=zh-cn"
        resp = httpx.get(url, timeout=timeout, headers={
            "User-Agent": "Mozilla/5.0 (compatible; LegalAI/1.0)"
        })
        resp.raise_for_status()
        html = resp.text
        return _parse_ddg_html(html, query)
    except Exception:
        return ""


def _parse_ddg_html(html: str, query: str) -> str:
    """Parse DuckDuckGo HTML results page."""
    try:
        lines = []
        import re
        results = re.findall(r'<a class="result__a"[^>]*href="([^"]*)"[^>]*>(.*?)</a>', html, re.DOTALL)
        snippets = re.findall(r'<a class="result__snippet"[^>]*>(.*?)</a>', html, re.DOTALL)
        for i, (href, title) in enumerate(results[:10]):
            title_clean = re.sub(r'<[^>]+>', '', title).strip()
            snippet = re.sub(r'<[^>]+>', '', snippets[i]) if i < len(snippets) else ""
            if title_clean and title_clean.lower() != query.lower():
                lines.append(f"【{title_clean}】{snippet}\n来源: {href}\n")
        return "\n".join(lines) if lines else ""
    except Exception:
        return ""


def _parse_serp_results(data: dict) -> str:
    """Parse results from paid search API (SerpAPI format)."""
    try:
        lines = []
        if "organic_results" in data:
            for item in data["organic_results"][:10]:
                title = item.get("title", "")
                snippet = item.get("snippet", "")
                link = item.get("link", "")
                if title:
                    lines.append(f"【{title}】{snippet}\n来源: {link}\n")
        elif "results" in data:
            for item in data["results"][:10]:
                lines.append(f"【{item.get('title', '')}】{item.get('snippet', '')}\n来源: {item.get('link', '')}\n")
        return "\n".join(lines) if lines else ""
    except Exception:
        return ""


# =============================================================================
# Tool Definitions
# =============================================================================

TOOLS = [
    # --- Existing 8 tools (preserved) ---
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

    # --- Direction A: case_detail & case_analyze ---
    Tool(
        name="case_detail",
        description="获取案例详情 - 根据案例UUID获取案例的完整信息，包括当事人、案由、审理程序、判决结果等。",
        inputSchema={
            "type": "object",
            "properties": {
                "caseUuid": {"type": "string", "description": "案例UUID（从case_search或case_similar结果中获取）"}
            },
            "required": ["caseUuid"]
        }
    ),
    Tool(
        name="case_analyze",
        description="AI案情分析 - 对案例进行AI智能分析，输出案情摘要、争议焦点、适用法律、判决理由等结构化分析结果。",
        inputSchema={
            "type": "object",
            "properties": {
                "caseUuid": {"type": "string", "description": "案例UUID（从case_detail或case_search结果中获取）"}
            },
            "required": ["caseUuid"]
        }
    ),

    # --- Direction A: contract dimensions, history, risk detail ---
    Tool(
        name="contract_dimensions",
        description="获取合同审查维度 - 查询AI合同审查支持的所有风险维度及其权重，包括主体资格、合同效力、权利义务等。",
        inputSchema={
            "type": "object",
            "properties": {}
        }
    ),
    Tool(
        name="contract_history",
        description="获取合同审查历史 - 查询当前用户的合同审查记录列表，包括审查时间、合同摘要、风险等级等。",
        inputSchema={
            "type": "object",
            "properties": {
                "limit": {"type": "integer", "description": "返回记录数量，默认20", "default": 20}
            }
        }
    ),
    Tool(
        name="contract_risk_detail",
        description="获取合同审查详情 - 根据审查UUID获取合同审查的完整结果，包括各维度评分、风险条款清单和修改建议。",
        inputSchema={
            "type": "object",
            "properties": {
                "reviewUuid": {"type": "string", "description": "审查记录UUID（从contract_history或contract_review返回结果中获取）"}
            },
            "required": ["reviewUuid"]
        }
    ),

    # --- Direction A: legal research sync + stream ---
    Tool(
        name="legal_research",
        description="法律研究（同步）- 输入法律问题，同步生成结构化法律研究报告，包括问题界定、法律依据、案例参考、风险提示和结论建议。",
        inputSchema={
            "type": "object",
            "properties": {
                "question": {"type": "string", "description": "研究问题或法律咨询主题，如'建筑工程合同纠纷中的工期延误责任'"},
                "depth": {"type": "string", "description": "研究深度：brief（简要）/standard（标准）/comprehensive（全面）", "default": "standard"},
                "sources": {"type": "array", "items": {"type": "string"}, "description": "指定的法律来源，如['民法典','劳动法']", "default": []}
            },
            "required": ["question"]
        }
    ),
    Tool(
        name="legal_research_stream",
        description="法律研究（流式）- 同legal_research，但通过SSE流式输出，可实时看到研究进度各阶段（解析问题、检索法规、生成报告等）。",
        inputSchema={
            "type": "object",
            "properties": {
                "question": {"type": "string", "description": "研究问题或法律咨询主题"},
                "depth": {"type": "string", "description": "研究深度：brief（简要）/standard（标准）/comprehensive（全面）", "default": "standard"},
                "sources": {"type": "array", "items": {"type": "string"}, "description": "指定的法律来源", "default": []}
            },
            "required": ["question"]
        }
    ),

    # --- Direction A: document drafting ---
    Tool(
        name="document_templates",
        description="获取文书模板列表 - 查询所有可用的法律文书模板，包括起诉状、答辩状、合同协议等各类模板。",
        inputSchema={
            "type": "object",
            "properties": {}
        }
    ),
    Tool(
        name="document_template_detail",
        description="获取文书模板详情 - 根据模板代码获取指定文书模板的详细信息，包括模板结构、必填字段说明。",
        inputSchema={
            "type": "object",
            "properties": {
                "templateCode": {"type": "string", "description": "模板代码（从document_templates结果中获取）"}
            },
            "required": ["templateCode"]
        }
    ),
    Tool(
        name="document_draft",
        description="起草法律文书 - 根据模板和案件信息生成法律文书，包括起诉状、答辩状、和解协议等。支持传入当事人信息、诉求金额、事实理由等。",
        inputSchema={
            "type": "object",
            "properties": {
                "templateCode": {"type": "string", "description": "模板代码（从document_templates获取，如'民事起诉状'）"},
                "caseType": {"type": "string", "description": "案件类型，如'民事'、'劳动争议'", "default": ""},
                "plaintiffName": {"type": "string", "description": "原告姓名", "default": ""},
                "plaintiffPhone": {"type": "string", "description": "原告电话", "default": ""},
                "plaintiffIdCard": {"type": "string", "description": "原告身份证号", "default": ""},
                "plaintiffAddress": {"type": "string", "description": "原告地址", "default": ""},
                "defendantName": {"type": "string", "description": "被告姓名", "default": ""},
                "defendantPhone": {"type": "string", "description": "被告电话", "default": ""},
                "defendantIdCard": {"type": "string", "description": "被告身份证号", "default": ""},
                "defendantAddress": {"type": "string", "description": "被告地址", "default": ""},
                "defendantCompany": {"type": "string", "description": "被告公司名称", "default": ""},
                "claimAmount": {"type": "number", "description": "诉讼金额（元）", "default": None},
                "claimDescription": {"type": "string", "description": "诉讼请求描述", "default": ""},
                "facts": {"type": "array", "items": {"type": "string"}, "description": "案件事实列表", "default": []},
                "evidence": {"type": "array", "items": {"type": "string"}, "description": "证据材料列表", "default": []},
                "courtName": {"type": "string", "description": "管辖法院名称", "default": ""},
                "caseCause": {"type": "string", "description": "案由", "default": ""},
                "includeRiskPrompt": {"type": "boolean", "description": "是否包含风险提示", "default": True}
            }
        }
    ),

    # --- Direction A: law analysis ---
    Tool(
        name="law_analyze",
        description="法规智能分析 - 对指定法规进行AI智能分析，输出法规要点解读、适用场景、关联法规、与相关法律的比较分析等。",
        inputSchema={
            "type": "object",
            "properties": {
                "lawUuid": {"type": "string", "description": "法规UUID（从law_search或get_law_detail结果中获取）"},
                "lawTitle": {"type": "string", "description": "法规标题（可选，与lawUuid二选一）", "default": ""},
                "articles": {"type": "array", "items": {"type": "object"}, "description": "具体条款内容（可选）", "default": []}
            }
        }
    ),

    # --- Direction A: doc_qa session management ---
    Tool(
        name="doc_qa_sessions",
        description="获取问答会话列表 - 查询当前用户的所有文档问答会话，返回会话ID列表和摘要信息。",
        inputSchema={
            "type": "object",
            "properties": {
                "userId": {"type": "string", "description": "用户ID（可选，默认使用default）", "default": "default"}
            }
        }
    ),
    Tool(
        name="doc_qa_create_session",
        description="创建问答会话 - 创建一个新的文档问答会话，返回会话ID，用于后续连续对话。",
        inputSchema={
            "type": "object",
            "properties": {
                "userId": {"type": "string", "description": "用户ID（可选，默认使用default）", "default": "default"}
            }
        }
    ),
    Tool(
        name="doc_qa_session_history",
        description="获取会话历史 - 查询指定会话的完整问答历史，包括用户问题和AI回答。",
        inputSchema={
            "type": "object",
            "properties": {
                "sessionId": {"type": "string", "description": "会话ID（从doc_qa_sessions或doc_qa_create_session结果中获取）"}
            },
            "required": ["sessionId"]
        }
    ),

    # --- Direction C: health check ---
    Tool(
        name="health_check",
        description="健康检查 - 检查法律AI助手后端服务的健康状态，包括数据库、Redis缓存等组件的连接状态。",
        inputSchema={
            "type": "object",
            "properties": {}
        }
    ),
    Tool(
        name="ai_status",
        description="AI服务状态 - 检查MiniMax AI模型服务的连接状态和可用性，返回online/offline状态。",
        inputSchema={
            "type": "object",
            "properties": {}
        }
    ),

    # --- Infrastructure: real web search ---
    Tool(
        name="web_search",
        description="网络搜索 - 通过 DuckDuckGo 实时搜索互联网，获取最新信息。用于查询企业工商信息、司法判决、新闻舆情等实时数据。支持任何关键词搜索。",
        inputSchema={
            "type": "object",
            "properties": {
                "query": {"type": "string", "description": "搜索关键词，如'腾讯公司工商信息'、'某企业法律诉讼'、'某公司失信被执行人'"}
            },
            "required": ["query"]
        }
    ),
]


# =============================================================================
# Tool Handlers
# =============================================================================

@server.list_tools()
async def list_tools() -> list[Tool]:
    return TOOLS


@server.call_tool()
async def call_tool(name: str, arguments: dict[str, Any]) -> list[TextContent]:
    result = await _dispatch(name, arguments)
    if isinstance(result, dict) and "error" in result:
        return [TextContent(type="text", text=f"Error: {result['error']}")]
    text = _format_result(name, result)
    return [TextContent(type="text", text=text)]


async def _dispatch(name: str, arguments: dict[str, Any]) -> dict[str, Any]:
    # --- Existing 8 tools ---
    if name == "legal_search":
        return _make_request("POST", "/api/v1/legal-search/search", {
            "query": arguments["query"],
            "page": arguments.get("page", 1),
            "pageSize": arguments.get("pageSize", 10),
            "includeCases": arguments.get("includeCases", False)
        })

    elif name == "case_similar":
        return _make_request("POST", "/api/v1/case-similar/search", {
            "description": arguments["description"],
            "caseType": arguments.get("caseType", ""),
            "limit": arguments.get("limit", 5)
        })

    elif name == "case_search":
        return _make_request("POST", "/api/v1/case-search/search", {
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
        return _make_request("POST", "/api/v1/company/query", {
            "companyName": arguments.get("companyName"),
            "unifiedSocialCreditCode": arguments.get("unifiedSocialCreditCode"),
            "enableRiskWarning": arguments.get("enableRiskWarning", True)
        })

    elif name == "contract_review":
        return _make_request("POST", "/api/v1/contract/review", {
            "text": arguments["text"],
            "templateCode": arguments.get("templateCode", ""),
            "dimensions": arguments.get("dimensions", [])
        })

    elif name == "law_search":
        return _make_request("POST", "/api/v1/law-search/search", {
            "keyword": arguments.get("keyword", ""),
            "status": arguments.get("status"),
            "page": arguments.get("page", 1),
            "pageSize": arguments.get("pageSize", 10)
        })

    elif name == "get_law_detail":
        return _make_request("GET", f"/api/v1/law-search/laws/{arguments['lawUuid']}")

    elif name == "doc_qa_ask":
        return _make_request("POST", "/api/v1/doc-qa/ask", {
            "question": arguments["question"],
            "kbId": arguments.get("kbId", ""),
            "sessionId": arguments.get("sessionId", "")
        })

    # --- Direction A: case ---
    elif name == "case_detail":
        return _make_request("GET", f"/api/v1/case-search/cases/{arguments['caseUuid']}")

    elif name == "case_analyze":
        return _make_request("GET", f"/api/v1/case-search/cases/{arguments['caseUuid']}/analysis")

    # --- Direction A: contract ---
    elif name == "contract_dimensions":
        return _make_request("GET", "/api/v1/contract/dimensions")

    elif name == "contract_history":
        return _make_request("GET", "/api/v1/contract/reviews", params={"limit": arguments.get("limit", 20)})

    elif name == "contract_risk_detail":
        return _make_request("GET", f"/api/v1/contract/reviews/{arguments['reviewUuid']}")

    # --- Direction A: legal research ---
    elif name == "legal_research":
        return _make_request("POST", "/api/v1/legal-research/generate", {
            "question": arguments["question"],
            "depth": arguments.get("depth", "standard"),
            "sources": arguments.get("sources", [])
        })

    elif name == "legal_research_stream":
        chunks = _stream_request("POST", "/api/v1/legal-research/generate/stream", {
            "question": arguments["question"],
            "depth": arguments.get("depth", "standard"),
            "sources": arguments.get("sources", [])
        })
        parsed = _parse_sse_events(chunks)
        return parsed

    # --- Direction A: document ---
    elif name == "document_templates":
        return _make_request("GET", "/api/v1/document/templates")

    elif name == "document_template_detail":
        return _make_request("GET", f"/api/v1/document/templates/{arguments['templateCode']}")

    elif name == "document_draft":
        case_data = {}
        for field in ["plaintiffName", "plaintiffPhone", "plaintiffIdCard", "plaintiffAddress",
                      "defendantName", "defendantPhone", "defendantIdCard", "defendantAddress",
                      "defendantCompany", "claimAmount", "claimDescription", "facts",
                      "evidence", "courtName", "caseCause"]:
            if field in arguments and arguments[field] not in (None, ""):
                case_data[field] = arguments[field]
        payload = {
            "templateCode": arguments.get("templateCode", ""),
            "caseType": arguments.get("caseType", ""),
            "caseData": case_data if case_data else None,
            "includeRiskPrompt": arguments.get("includeRiskPrompt", True)
        }
        return _make_request("POST", "/api/v1/document/draft", payload)

    # --- Direction A: law analysis ---
    elif name == "law_analyze":
        payload = {}
        if arguments.get("lawUuid"):
            payload["lawUuid"] = arguments["lawUuid"]
        if arguments.get("lawTitle"):
            payload["lawTitle"] = arguments["lawTitle"]
        if arguments.get("articles"):
            payload["articles"] = arguments["articles"]
        return _make_request("POST", "/api/v1/law-analysis/analyze", payload)

    # --- Direction A: doc_qa sessions ---
    elif name == "doc_qa_sessions":
        return _make_request("GET", "/api/v1/doc-qa/sessions", params={"userId": arguments.get("userId", "default")})

    elif name == "doc_qa_create_session":
        return _make_request("POST", "/api/v1/doc-qa/sessions", params={"userId": arguments.get("userId", "default")})

    elif name == "doc_qa_session_history":
        return _make_request("GET", f"/api/v1/doc-qa/sessions/{arguments['sessionId']}/history")

    # --- Direction C: health ---
    elif name == "health_check":
        return _make_request("GET", "/api/v1/health")

    elif name == "ai_status":
        return _make_request("GET", "/api/v1/ai-status")

    elif name == "web_search":
        search_results = _search_web(arguments["query"])
        return {"webSearchResult": search_results, "query": arguments["query"]}

    else:
        return {"error": f"Unknown tool: {name}"}


# =============================================================================
# Formatters
# =============================================================================

def _format_result(name: str, result: Any) -> str:
    if not result:
        return "未找到相关结果"

    if isinstance(result, dict) and result.get("error"):
        return f"错误: {result['error']}"

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

    # --- Direction A: case ---
    elif name == "case_detail":
        if isinstance(result, dict):
            title = result.get("caseName", result.get("title", ""))
            court = result.get("court", "")
            case_type = result.get("caseType", "")
            procedure = result.get("trialProcedure", "")
            date = result.get("judgmentDate", "")
            judgment = result.get("judgmentResult", "")
            lines = [f"案例详情：{title}\n"]
            if case_type:
                lines.append(f"  案件类型：{case_type}")
            if court:
                lines.append(f"  审理法院：{court}")
            if procedure:
                lines.append(f"  审理程序：{procedure}")
            if date:
                lines.append(f"  判决日期：{date}")
            if judgment:
                lines.append(f"\n判决结果：\n{judgment}")
            return "\n".join(lines)
        return str(result)

    elif name == "case_analyze":
        if isinstance(result, dict):
            summary = result.get("summary", result.get("caseSummary", ""))
            focus = result.get("disputeFocus", result.get("争议焦点", ""))
            laws = result.get("applicableLaws", result.get("适用法律", []))
            reasoning = result.get("judgmentReason", result.get("判决理由", ""))
            lines = ["AI案情分析：\n"]
            if summary:
                lines.append(f"一、案情摘要\n{summary}\n")
            if focus:
                lines.append(f"二、争议焦点\n{focus}\n")
            if laws:
                law_text = "\n".join(f"  - {l}" for l in (laws if isinstance(laws, list) else [laws])) if isinstance(laws, list) else f"  {laws}"
                lines.append(f"三、适用法律\n{laws}\n")
            if reasoning:
                lines.append(f"四、判决理由\n{reasoning}\n")
            return "\n".join(lines)
        if isinstance(result, str):
            return f"AI案情分析：\n{result}"
        return str(result)

    # --- Direction A: contract ---
    elif name == "contract_dimensions":
        if isinstance(result, list):
            lines = ["合同审查维度（风险权重）：\n"]
            for dim in result:
                code = dim.get("dimensionCode", "")
                name_d = dim.get("dimensionName", "")
                weight = dim.get("weight", 0)
                lines.append(f"  【{name_d}】{weight}% ({code})")
            return "\n".join(lines)
        return str(result)

    elif name == "contract_history":
        if isinstance(result, list):
            if not result:
                return "暂无合同审查记录"
            lines = ["合同审查历史：\n"]
            for item in result[:10]:
                date = item.get("createdAt", item.get("createTime", ""))
                risk = item.get("overallRisk", item.get("riskLevel", ""))
                text = item.get("contractText", item.get("text", ""))[:50]
                lines.append(f"  [{date}] 风险:{risk} | {text}...")
            return "\n".join(lines)
        return str(result)

    elif name == "contract_risk_detail":
        if isinstance(result, dict):
            return _format_result("contract_review", result)
        return str(result)

    # --- Direction A: legal research ---
    elif name == "legal_research":
        if isinstance(result, dict):
            question = result.get("question", "")
            definition = result.get("definition", result.get("问题界定", ""))
            laws = result.get("legalBasis", result.get("法律依据", []))
            cases = result.get("caseReferences", result.get("案例参考", []))
            risks = result.get("risk提示", result.get("riskWarnings", []))
            conclusion = result.get("conclusion", result.get("结论建议", ""))
            lines = [f"法律研究报告：{question}\n"]
            if definition:
                lines.append(f"\n一、问题界定\n{definition}\n")
            if laws:
                laws_text = "\n".join(f"  - {l}" for l in (laws if isinstance(laws, list) else [laws])) if isinstance(laws, list) else f"  {laws}"
                lines.append(f"二、法律依据\n{laws_text}\n")
            if cases:
                cases_text = "\n".join(f"  - {c}" for c in (cases if isinstance(cases, list) else [cases])) if isinstance(cases, list) else f"  {cases}"
                lines.append(f"三、案例参考\n{cases_text}\n")
            if risks:
                risks_text = "\n".join(f"  - {r}" for r in (risks if isinstance(risks, list) else [risks])) if isinstance(risks, list) else f"  {risks}"
                lines.append(f"四、风险提示\n{risks_text}\n")
            if conclusion:
                lines.append(f"五、结论建议\n{conclusion}\n")
            return "\n".join(lines)
        if isinstance(result, str):
            return f"法律研究报告：\n{result}"
        return str(result)

    elif name == "legal_research_stream":
        phases = result.get("phases", [])
        content = result.get("content", "")
        error = result.get("error")
        if error:
            return f"法律研究错误：{error}"
        lines = ["法律研究进度：\n"]
        for ph in phases:
            phase = ph.get("phase", "")
            progress = ph.get("progress", 0)
            message = ph.get("message", "")
            phase_map = {"parse": "解析问题", "search_laws": "检索法规", "search_cases": "检索案例",
                         "generate_def": "生成问题界定", "generate_basis": "生成法律依据",
                         "generate_risk": "生成风险提示", "generate_conclusion": "生成结论建议",
                         "complete": "完成"}
            lines.append(f"  [{progress:3d}%] {phase_map.get(phase, phase)} - {message}")
        if content:
            lines.append(f"\n研究报告：\n{content[:2000]}")
        return "\n".join(lines)

    # --- Direction A: document ---
    elif name == "document_templates":
        if isinstance(result, list):
            lines = ["法律文书模板：\n"]
            for t in result:
                code = t.get("templateCode", "")
                name_d = t.get("templateName", t.get("name", ""))
                category = t.get("category", "")
                lines.append(f"  【{name_d}】{category} ({code})")
            return "\n".join(lines)
        return str(result)

    elif name == "document_template_detail":
        if isinstance(result, dict):
            code = result.get("templateCode", "")
            name_d = result.get("templateName", result.get("name", ""))
            desc = result.get("description", result.get("desc", ""))
            fields = result.get("fields", result.get("requiredFields", []))
            lines = [f"模板：{name_d} ({code})\n"]
            if desc:
                lines.append(f"说明：{desc}\n")
            if fields:
                fields_text = "\n".join(f"  - {f}" for f in (fields if isinstance(fields, list) else [fields])) if isinstance(fields, list) else f"  {fields}"
                lines.append(f"必填字段：\n{fields_text}")
            return "\n".join(lines)
        return str(result)

    elif name == "document_draft":
        if isinstance(result, dict):
            content = result.get("documentContent", "")
            risk = result.get("riskPrompt", "")
            disclaimer = result.get("disclaimer", "")
            laws = result.get("referencedLaws", [])
            lines = ["起草的法律文书：\n"]
            lines.append(f"{content}\n")
            if risk:
                lines.append(f"\n风险提示：{risk}\n")
            if disclaimer:
                lines.append(f"免责声明：{disclaimer}\n")
            if laws:
                laws_text = "\n".join(f"  - {l}" for l in laws) if isinstance(laws, list) else f"  {laws}"
                lines.append(f"\n引用法规：\n{laws_text}")
            return "\n".join(lines)
        return str(result)

    # --- Direction A: law analysis ---
    elif name == "law_analyze":
        if isinstance(result, dict):
            key_points = result.get("keyPoints", result.get("要点解读", ""))
            scenarios = result.get("applicableScenarios", result.get("适用场景", []))
            related = result.get("relatedLaws", result.get("关联法规", []))
            comparison = result.get("comparison", result.get("比较分析", ""))
            lines = ["法规分析结果：\n"]
            if key_points:
                lines.append(f"一、要点解读\n{key_points}\n")
            if scenarios:
                scenes_text = "\n".join(f"  - {s}" for s in (scenarios if isinstance(scenarios, list) else [scenarios])) if isinstance(scenarios, list) else f"  {scenarios}"
                lines.append(f"二、适用场景\n{scenes_text}\n")
            if related:
                related_text = "\n".join(f"  - {r}" for r in (related if isinstance(related, list) else [related])) if isinstance(related, list) else f"  {related}"
                lines.append(f"三、关联法规\n{related_text}\n")
            if comparison:
                lines.append(f"四、比较分析\n{comparison}\n")
            return "\n".join(lines)
        if isinstance(result, str):
            return f"法规分析结果：\n{result}"
        return str(result)

    # --- Direction A: doc_qa sessions ---
    elif name == "doc_qa_sessions":
        if isinstance(result, list):
            if not result:
                return "暂无问答会话"
            lines = ["问答会话列表：\n"]
            for s in result[:10]:
                sid = s.get("sessionId", s.get("id", ""))
                updated = s.get("updatedAt", s.get("lastMessage", ""))
                preview = s.get("lastMessage", s.get("preview", ""))[:50]
                lines.append(f"  [{sid}] {updated} - {preview}...")
            return "\n".join(lines)
        return str(result)

    elif name == "doc_qa_create_session":
        if isinstance(result, dict):
            sid = result.get("sessionId", result.get("id", ""))
            return f"新会话已创建，sessionId：{sid}"
        return str(result)

    elif name == "doc_qa_session_history":
        if isinstance(result, list):
            if not result:
                return "该会话暂无历史记录"
            lines = ["会话历史：\n"]
            for msg in result:
                role = msg.get("role", "")
                content = msg.get("content", "")
                label = "用户" if role == "user" else "AI"
                lines.append(f"【{label}】：{content}\n")
            return "\n".join(lines)
        return str(result)

    # --- Direction C: health ---
    elif name == "health_check":
        if isinstance(result, dict):
            status = result.get("status", result.get("health", ""))
            db = result.get("database", result.get("db", ""))
            redis = result.get("redis", result.get("cache", ""))
            lines = [f"服务健康状态：{status}\n"]
            if db:
                lines.append(f"  数据库：{db}")
            if redis:
                lines.append(f"  缓存：{redis}")
            return "\n".join(lines)
        return str(result)

    elif name == "ai_status":
        if isinstance(result, dict):
            ai_status = result.get("status", result.get("aiStatus", ""))
            model = result.get("model", result.get("currentModel", ""))
            lines = [f"AI服务状态：{ai_status}"]
            if model:
                lines.append(f"  当前模型：{model}")
            return "\n".join(lines)
        return str(result)

    elif name == "web_search":
        if isinstance(result, dict):
            search_result = result.get("webSearchResult", result.get("content", ""))
            query = result.get("query", "")
            return f"网络搜索结果（关键词：{query}）：\n\n{search_result}"
        return str(result)

    else:
        return str(result)


# =============================================================================
# Server Entry Point
# =============================================================================

mcp = server


async def main():
    async with stdio_server() as (read_stream, write_stream):
        await server.run(
            read_stream,
            write_stream,
            server.create_initialization_options()
        )


if __name__ == "__main__":
    import asyncio
    asyncio.run(main())
