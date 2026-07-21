import json
from .models import StructureResult, ClassificationResult, CategorySuggestion
from .client import MiniMaxClient

CLASSIFY_PROMPT = """你是一个专业的中国法律法规分类助手。请根据法规名称和内容，判断其一级和二级分类。

常见一级分类：宪法、民法商法、行政法、经济法、社会法、刑法、诉讼与非诉讼程序法、劳动法、土地法、环境法

请根据法规名称和内容推断最合适的一级和二级分类。

输出严格 JSON 格式：
{
  "suggested_categories": [
    {"type_name": "一级分类名", "category_name": "二级分类名", "confidence": 0.85}
  ]
}"""

CLASSIFIER_KEYWORDS = {
    "宪法": "宪法",
    "民法典": "民法商法",
    "合同法": "民法商法",
    "公司法": "民法商法",
    "劳动法": "劳动法",
    "劳动合同法": "劳动法",
    "社会保险法": "劳动法",
    "刑法": "刑法",
    "刑事诉讼法": "诉讼与非诉讼程序法",
    "民事诉讼法": "诉讼与非诉讼程序法",
    "行政诉讼法": "诉讼与非诉讼程序法",
    "土地管理法": "土地法",
    "房地产法": "土地法",
    "环境保护法": "环境法",
    "大气污染防治法": "环境法",
    "水污染防治法": "环境法",
    "企业所得税法": "经济法",
    "个人所得税法": "经济法",
    "增值税法": "经济法",
    "行政处罚法": "行政法",
    "行政复议法": "行政法",
    "公务员法": "行政法",
}

class Classifier:
    def __init__(self, client: MiniMaxClient):
        self.client = client

    def classify(self, result: StructureResult) -> ClassificationResult:
        title = result.law_title
        categories = []

        for keyword, cat_l1 in CLASSIFIER_KEYWORDS.items():
            if keyword in title:
                categories.append(CategorySuggestion(
                    type_name=cat_l1,
                    category_name="",
                    confidence=0.95 if len(keyword) > 4 else 0.80
                ))
                break

        if not categories:
            prompt = CLASSIFY_PROMPT + f"\n\n=== 法规名称 ===\n{title}\n\n=== 首条条款 ===\n{result.articles[0].content[:500] if result.articles else '无'}"
            try:
                response = self.client.chat(prompt)
                data = json.loads(response, strict=False)
                for cat in data.get("suggested_categories", []):
                    categories.append(CategorySuggestion(**cat))
            except (json.JSONDecodeError, KeyError):
                categories.append(CategorySuggestion(type_name="其他", category_name="", confidence=0.5))

        return ClassificationResult(suggested_categories=categories[:3])
