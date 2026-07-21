import pytest
from law_parser.ai.structure_extractor import StructureExtractor, StructureResult
from law_parser.ai.models import ArticleParse, CategorySuggestion, ClassificationResult

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
