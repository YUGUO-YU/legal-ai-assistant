from pydantic import BaseModel, Field
from typing import Optional

class ChapterNode(BaseModel):
    title: str
    level: int = 1
    children: list["ChapterNode"] = Field(default_factory=list)

class ArticleParse(BaseModel):
    article_no: str
    title: Optional[str] = None
    content: str
    chapter_path: str = ""
    sort_order: int = 0
    content_hash: Optional[str] = None
    review_warnings: list[str] = Field(default_factory=list)

class StructureResult(BaseModel):
    law_title: str
    short_title: Optional[str] = None
    issuing_authority: Optional[str] = None
    issue_date: Optional[str] = None
    effective_date: Optional[str] = None
    document_no: Optional[str] = None
    chapter_tree: list[ChapterNode] = Field(default_factory=list)
    articles: list[ArticleParse] = Field(default_factory=list)

class CategorySuggestion(BaseModel):
    type_name: str = ""
    category_name: str = ""
    confidence: float = 0.0

class ClassificationResult(BaseModel):
    suggested_categories: list[CategorySuggestion] = Field(default_factory=list)
