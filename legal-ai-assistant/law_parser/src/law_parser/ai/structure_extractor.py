import json, hashlib
from .models import StructureResult, ArticleParse, ChapterNode
from .client import MiniMaxClient

STRUCTURE_PROMPT = """你是一个专业的中国法律法规解析助手。请分析以下法律文档，提取结构信息。

要求：
1. 识别法规全称、简称、发布机关、发布日期、效力日期、文号
2. 识别章节结构（章、节）
3. 识别每一条条款的编号、标题（如有）、内容
4. 条款编号格式如"第X条"，内容到下一条款编号前为止

输出严格 JSON 格式：
{
  "law_title": "...",
  "short_title": "...",
  "issuing_authority": "...",
  "issue_date": "YYYY-MM-DD",
  "effective_date": "YYYY-MM-DD",
  "document_no": "...",
  "chapter_tree": [{"title": "...", "level": 1, "children": [...]}],
  "articles": [{"article_no": "第一条", "title": "...", "content": "...", "chapter_path": "...", "sort_order": 1}]
}

请确保 JSON 格式正确，可直接用 json.loads 解析。"""

class StructureExtractor:
    def __init__(self, client: MiniMaxClient):
        self.client = client

    def extract(self, text: str) -> StructureResult:
        response = self.client.chat(STRUCTURE_PROMPT + f"\n\n=== 文档内容 ===\n{text[:15000]}")
        try:
            data = json.loads(response, strict=False)
        except json.JSONDecodeError as e:
            raise ValueError(f"AI 返回非有效 JSON: {e}\n原始返回: {response[:500]}")

        articles = []
        for i, a in enumerate(data.get("articles", [])):
            content_hash = hashlib.sha256(a.get("content", "").encode()).hexdigest()[:16]
            articles.append(ArticleParse(
                article_no=a.get("article_no", ""),
                title=a.get("title"),
                content=a.get("content", ""),
                chapter_path=a.get("chapter_path", ""),
                sort_order=a.get("sort_order", i + 1),
                content_hash=content_hash
            ))

        return StructureResult(
            law_title=data.get("law_title", ""),
            short_title=data.get("short_title"),
            issuing_authority=data.get("issuing_authority"),
            issue_date=data.get("issue_date"),
            effective_date=data.get("effective_date"),
            document_no=data.get("document_no"),
            chapter_tree=[ChapterNode(**c) for c in data.get("chapter_tree", [])],
            articles=articles
        )
