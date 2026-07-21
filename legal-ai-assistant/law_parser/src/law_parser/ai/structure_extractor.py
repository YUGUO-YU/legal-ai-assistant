import json, hashlib
from .models import StructureResult, ArticleParse, ChapterNode
from .client import MiniMaxClient

STRUCTURE_PROMPT = """你是一个专业的中国法律法规解析助手。请分析以下法律文档，提取结构信息。

要求：
1. 识别法规全称、简称、发布机关、发布日期、效力日期、文号
2. 识别章节结构（篇/编/章/节）
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

MAX_CHUNK_SIZE = 12000


def _chunk_text(text: str, max_size: int = MAX_CHUNK_SIZE) -> list[str]:
    if len(text) <= max_size:
        return [text]
    chunks = []
    lines = text.split("\n")
    current_chunk = []
    current_size = 0
    for line in lines:
        if current_size + len(line) > max_size and current_chunk:
            chunks.append("\n".join(current_chunk))
            current_chunk = []
            current_size = 0
        current_chunk.append(line)
        current_size += len(line)
    if current_chunk:
        chunks.append("\n".join(current_chunk))
    return chunks


class StructureExtractor:
    def __init__(self, client: MiniMaxClient):
        self.client = client

    def extract(self, text: str) -> StructureResult:
        if len(text) <= MAX_CHUNK_SIZE:
            return self._extract_single(text)
        return self._extract_chunked(text)

    def _extract_single(self, text: str) -> StructureResult:
        response = self.client.chat(STRUCTURE_PROMPT + "\n\n=== 文档内容 ===\n" + text)
        return self._parse_response(response)

    def _extract_chunked(self, text: str) -> StructureResult:
        chunks = _chunk_text(text)
        all_articles = []
        law_title = ""
        short_title = ""
        issuing_authority = ""
        issue_date = ""
        effective_date = ""
        document_no = ""
        chapter_tree = []

        for i, chunk in enumerate(chunks):
            prompt = STRUCTURE_PROMPT + f"\n\n=== 文档内容（第 {i+1}/{len(chunks)} 部分）===\n" + chunk
            response = self.client.chat(prompt)
            try:
                data = json.loads(response, strict=False)
                if not law_title and data.get("law_title"):
                    law_title = data.get("law_title", "")
                    short_title = data.get("short_title", "")
                    issuing_authority = data.get("issuing_authority", "")
                    issue_date = data.get("issue_date", "")
                    effective_date = data.get("effective_date", "")
                    document_no = data.get("document_no", "")
                    chapter_tree = [ChapterNode(**c) for c in data.get("chapter_tree", [])]
                for a in data.get("articles", []):
                    all_articles.append(a)
            except json.JSONDecodeError:
                pass

        deduped = {}
        for a in all_articles:
            key = a.get("article_no", "")
            if key and key not in deduped:
                deduped[key] = a

        articles = []
        for i, (key, a) in enumerate(deduped.items()):
            content_hash = hashlib.sha256(a.get("content", "").encode()).hexdigest()[:16]
            articles.append(ArticleParse(
                article_no=a.get("article_no", ""),
                title=a.get("title"),
                content=a.get("content", ""),
                chapter_path=a.get("chapter_path", ""),
                sort_order=a.get("sort_order", i + 1),
                content_hash=content_hash
            ))

        articles.sort(key=lambda x: x.sort_order)
        for i, a in enumerate(articles):
            a.sort_order = i + 1

        if not law_title:
            lines = text.split("\n")
            for line in lines:
                if line.strip():
                    law_title = line.strip()
                    break
            if not law_title:
                law_title = "未知标题"

        return StructureResult(
            law_title=law_title,
            short_title=short_title,
            issuing_authority=issuing_authority,
            issue_date=issue_date,
            effective_date=effective_date,
            document_no=document_no,
            chapter_tree=chapter_tree,
            articles=articles
        )

    def _parse_response(self, response: str) -> StructureResult:
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
