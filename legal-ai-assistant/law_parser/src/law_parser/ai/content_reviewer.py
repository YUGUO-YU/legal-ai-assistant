import json, logging
from .models import StructureResult, ArticleParse

logger = logging.getLogger(__name__)

REVIEW_PROMPT = """你是一个专业的中国法律法规审查助手。请审查以下已解析的法规条款，修正格式错误并检查问题。

审查要求：
1. 检查条款编号是否连续（缺失编号报警告）
2. 修正全角/半角标点和格式问题
3. 检测重复条款（内容高度相似报警告）
4. 如条款无标题，从内容首句推断标题

原始解析结果（JSON）：
{structure_json}

输出严格 JSON 格式（只修改有问题的 articles）：
{{
  "warnings": ["第5条缺失", "第8条与第3条内容重复"],
  "corrected_articles": [{{"article_no": "...", "title": "...修正标题", "content": "...修正内容", "warnings": []}}]
}}"""


class ContentReviewer:
    def __init__(self, client):
        self.client = client

    def review(self, result: StructureResult) -> StructureResult:
        articles_json = json.dumps([{
            "article_no": a.article_no,
            "title": a.title,
            "content": a.content,
            "sort_order": a.sort_order
        } for a in result.articles], ensure_ascii=False, indent=2)

        response = self.client.chat(REVIEW_PROMPT.format(structure_json=articles_json))
        try:
            review_data = json.loads(response, strict=False)
        except json.JSONDecodeError as e:
            logger.warning("Content review AI returned invalid JSON, skipping review for law '%s': %s",
                result.law_title, e)
            for a in result.articles:
                a.review_warnings = []
            return result

        warnings = review_data.get("warnings", [])
        corrected = {c["article_no"]: c for c in review_data.get("corrected_articles", [])}

        for article in result.articles:
            corrected_article = corrected.get(article.article_no)
            if corrected_article:
                article.title = corrected_article.get("title") or article.title
                article.content = corrected_article.get("content") or article.content
            article.review_warnings = [w for w in warnings if w.startswith(article.article_no)]

        return result
