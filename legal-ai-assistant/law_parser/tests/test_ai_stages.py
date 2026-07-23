import pytest, json
from unittest.mock import patch, MagicMock
from law_parser.ai.structure_extractor import StructureExtractor, _chunk_text, _is_article_start
from law_parser.ai.content_reviewer import ContentReviewer
from law_parser.ai.classifier import Classifier
from law_parser.ai.models import StructureResult, ArticleParse, CategorySuggestion, ClassificationResult

def test_structure_result_model():
    result = StructureResult(
        law_title="民法典",
        short_title="民法典",
        issuing_authority="全国人大",
        issue_date="2020-05-28",
        effective_date="2021-01-01",
        document_no="主席令第四十五号",
        chapter_tree=[],
        articles=[]
    )
    assert result.law_title == "民法典"
    assert result.articles == []

def test_article_parse_model():
    article = ArticleParse(
        article_no="第一条",
        title="立法目的",
        content="为了...",
        sort_order=1
    )
    assert article.article_no == "第一条"
    assert article.review_warnings == []

def test_classification_result_model():
    result = ClassificationResult(suggested_categories=[
        CategorySuggestion(type_name="民法商法", confidence=0.95)
    ])
    assert len(result.suggested_categories) == 1

def test_structure_extractor_extract():
    mock_client = MagicMock()
    mock_client.chat.return_value = json.dumps({
        "law_title": "中华人民共和国劳动法",
        "short_title": "劳动法",
        "issuing_authority": "全国人大",
        "issue_date": "2018-12-29",
        "effective_date": "2019-01-01",
        "document_no": "主席令第二十四号",
        "chapter_tree": [],
        "articles": [
            {"article_no": "第一条", "title": "立法目的", "content": "为了...", "sort_order": 1}
        ]
    })
    extractor = StructureExtractor(mock_client)
    result = extractor.extract("测试内容")
    assert result.law_title == "中华人民共和国劳动法"
    assert len(result.articles) == 1
    assert result.articles[0].article_no == "第一条"

def test_content_reviewer_returns_warnings():
    mock_client = MagicMock()
    mock_client.chat.return_value = json.dumps({
        "warnings": ["第三条疑似重复"],
        "corrected_articles": []
    })
    reviewer = ContentReviewer(mock_client)
    result = StructureResult(law_title="劳动法", articles=[
        ArticleParse(article_no="第三条", title="", content="内容...", sort_order=3)
    ])
    reviewed = reviewer.review(result)
    assert len(reviewed.articles[0].review_warnings) == 1

def test_classifier_keyword_match():
    mock_client = MagicMock()
    mock_client.chat.return_value = json.dumps({
        "suggested_categories": [
            {"type_name": "劳动法", "category_name": "劳动争议", "confidence": 0.98}
        ]
    })
    classifier = Classifier(mock_client)
    result = StructureResult(law_title="中华人民共和国劳动合同法", articles=[
        ArticleParse(article_no="第一条", content="为了...", sort_order=1)
    ])
    classes = classifier.classify(result)
    assert len(classes.suggested_categories) >= 1
    assert classes.suggested_categories[0].type_name == "劳动法"

def test_classifier_multiple_keywords():
    mock_client = MagicMock()
    mock_client.chat.return_value = json.dumps({"suggested_categories": []})
    classifier = Classifier(mock_client)
    result = StructureResult(law_title="中华人民共和国企业所得税法和个人所得税法", articles=[])
    classes = classifier.classify(result)
    assert len(classes.suggested_categories) >= 1
    categories = [c.type_name for c in classes.suggested_categories]
    assert "经济法" in categories

def test_classifier_keyword_case_sensitive():
    mock_client = MagicMock()
    mock_client.chat.return_value = json.dumps({"suggested_categories": []})
    classifier = Classifier(mock_client)
    result = StructureResult(law_title="中华人民共和国劳动合同法", articles=[])
    classes = classifier.classify(result)
    assert len(classes.suggested_categories) >= 1
    assert classes.suggested_categories[0].type_name == "劳动法"

def test_classifier_empty_title():
    mock_client = MagicMock()
    mock_client.chat.return_value = json.dumps({"suggested_categories": []})
    classifier = Classifier(mock_client)
    result = StructureResult(law_title="", articles=[
        ArticleParse(article_no="第一条", content="内容", sort_order=1)
    ])
    classes = classifier.classify(result)
    assert len(classes.suggested_categories) >= 1
    assert classes.suggested_categories[0].type_name == "其他"

def test_classifier_no_articles():
    mock_client = MagicMock()
    mock_client.chat.return_value = json.dumps({"suggested_categories": []})
    classifier = Classifier(mock_client)
    result = StructureResult(law_title="中华人民共和国公司法", articles=[])
    classes = classifier.classify(result)
    assert len(classes.suggested_categories) >= 1
    assert classes.suggested_categories[0].type_name == "民法商法"

def test_classifier_ai_fallback():
    mock_client = MagicMock()
    mock_client.chat.return_value = json.dumps({
        "suggested_categories": [
            {"type_name": "行政法", "category_name": "", "confidence": 0.8}
        ]
    })
    classifier = Classifier(mock_client)
    result = StructureResult(law_title="无关键词法律", articles=[
        ArticleParse(article_no="第一条", content="内容", sort_order=1)
    ])
    classes = classifier.classify(result)
    assert len(classes.suggested_categories) >= 1

def test_structure_extractor_large_doc_chunking():
    mock_client = MagicMock()
    responses = [
        json.dumps({"law_title": "劳动法", "articles": [{"article_no": "第一条", "content": "内容1", "sort_order": 1}]}),
        json.dumps({"law_title": "劳动法", "articles": [{"article_no": "第二条", "content": "内容2", "sort_order": 2}]}),
    ]
    mock_client.chat.side_effect = responses
    extractor = StructureExtractor(mock_client)
    large_text = ("x" * 5000 + "\n") * 3
    result = extractor.extract(large_text)
    assert len(result.articles) == 2

def test_content_reviewer_invalid_json():
    mock_client = MagicMock()
    mock_client.chat.return_value = "这不是JSON"
    reviewer = ContentReviewer(mock_client)
    result = StructureResult(law_title="劳动法", articles=[
        ArticleParse(article_no="第一条", content="内容", sort_order=1)
    ])
    reviewed = reviewer.review(result)
    assert len(reviewed.articles[0].review_warnings) == 0


def test_is_article_start():
    assert _is_article_start("第一条 为了保护民事主体") == True
    assert _is_article_start("第二十条 父母对子女") == True
    assert _is_article_start("根据本法规定") == False
    assert _is_article_start("第一章 总则") == False
    assert _is_article_start("") == False


def test_chunk_text_respects_article_boundaries():
    article1 = ("第一条 为了保护民事主体的合法权益，调整民事关系，适应中国特色社会主义事业发展需要。" + "\n") * 300
    article2 = ("第二条 民事主体的人身权利、财产权利以及其他合法权益受法律保护，任何组织或者个人不得侵犯。" + "\n") * 300
    article3 = ("第三条 民事主体在民事活动中的法律地位一律平等。" + "\n") * 300
    combined = article1 + article2 + article3
    chunks = _chunk_text(combined, max_size=6000)
    combined_text = "".join(chunks)
    assert "第一条" in combined_text
    assert "第二条" in combined_text
    assert "第三条" in combined_text
