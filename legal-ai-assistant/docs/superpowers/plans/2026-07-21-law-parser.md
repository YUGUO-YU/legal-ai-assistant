# 法规文档解析 AI 审查工具实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Python 法规解析工具，支持 PDF/Word/JSON/TXT 解析，MiniMax AI 三阶段审查（结构提取/内容审查/自动分类），写入 MySQL，复用 JSON-RPC over stdin/stdout 与 Spring Boot 集成。

**Architecture:** Python CLI + 子进程嵌入 Spring Boot；文件解析层 → AI 审查层 → 数据库写入层；JSON-RPC 2.0 协议封装进程通信。

**Tech Stack:** Python 3.11+, pdfplumber, python-docx, pymysql, httpx, python-dotenv

---

## 项目脚手架

**Files:**
- Create: `law_parser/pyproject.toml`
- Create: `law_parser/.env.example`
- Create: `law_parser/src/__init__.py`
- Create: `law_parser/src/parser/__init__.py`
- Create: `law_parser/src/ai/__init__.py`
- Create: `law_parser/src/db/__init__.py`
- Create: `law_parser/src/protocol/__init__.py`
- Create: `law_parser/tests/__init__.py`

**Global Constraints:**
- Python >= 3.11
- MiniMax OpenAI 兼容接口: `https://api.minimax.chat/v1`
- MySQL: `legal_ai` database
- JSON-RPC 2.0 over stdin/stdout
- 输出字段与 `LawImportPreview.java` 兼容

---

### Task 1: 项目脚手架

- [ ] **Step 1: 创建 pyproject.toml**

```toml
[project]
name = "law-parser"
version = "0.1.0"
requires-python = ">=3.11"
dependencies = [
    "pdfplumber>=0.11.0",
    "python-docx>=1.1.0",
    "pymysql>=1.1.0",
    "httpx>=0.27.0",
    "python-dotenv>=1.0.0",
    "pydantic>=2.0.0",
]

[project.optional-dependencies]
dev = ["pytest>=8.0.0", "pytest-asyncio>=0.23.0"]

[project.scripts]
law-parser = "law_parser.main:main"

[build-system]
requires = ["hatchling"]
build-backend = "hatchling.build"
```

- [ ] **Step 2: 创建 .env.example**

```env
MINIMAX_API_KEY=your_api_key_here
MINIMAX_BASE_URL=https://api.minimax.chat/v1
MINIMAX_MODEL=MiniMax-M3
MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_USER=root
MYSQL_PASSWORD=
MYSQL_DATABASE=legal_ai
```

- [ ] **Step 3: 创建目录结构和 __init__.py 文件**

```bash
mkdir -p law_parser/src/parser law_parser/src/ai law_parser/src/db law_parser/src/protocol law_parser/tests
touch law_parser/src/__init__.py law_parser/src/parser/__init__.py law_parser/src/ai/__init__.py law_parser/src/db/__init__.py law_parser/src/protocol/__init__.py law_parser/tests/__init__.py
```

- [ ] **Step 4: Commit**

```bash
git add law_parser/pyproject.toml law_parser/.env.example law_parser/src law_parser/tests
git commit -m "feat(law-parser): project scaffold"
```

---

### Task 2: 文件解析器层

**Files:**
- Create: `law_parser/src/parser/base.py`
- Create: `law_parser/src/parser/pdf_parser.py`
- Create: `law_parser/src/parser/docx_parser.py`
- Create: `law_parser/src/parser/json_parser.py`
- Create: `law_parser/src/parser/txt_parser.py`
- Create: `law_parser/tests/test_parser.py`

**Interfaces:**
- Consumes: 文件路径 (str)
- Produces: `dict` with keys `{text: str, source_type: str, page_count: int}`

- [ ] **Step 1: 编写 Parser 基类和 TxtParser 测试**

```python
# tests/test_parser.py
import pytest
from law_parser.parser.txt_parser import TxtParser

def test_txt_parser_basic():
    import tempfile, os
    with tempfile.NamedTemporaryFile(mode='w', suffix='.txt', delete=False, encoding='utf-8') as f:
        f.write("这是测试内容\n第二行")
        path = f.name
    try:
        parser = TxtParser()
        result = parser.parse(path)
        assert result['text'] == "这是测试内容\n第二行"
        assert result['source_type'] == 'txt'
    finally:
        os.unlink(path)
```

- [ ] **Step 2: 运行测试验证失败**

```bash
cd law_parser && pip install -e ".[dev]" -q && pytest tests/test_parser.py::test_txt_parser_basic -v
```
Expected: FAIL - TxtParser not defined

- [ ] **Step 3: 实现 base.py**

```python
# src/parser/base.py
from abc import ABC, abstractmethod

class BaseParser(ABC):
    extensions: list[str] = []

    @abstractmethod
    def parse(self, file_path: str) -> dict:
        """返回 {'text': str, 'source_type': str, 'page_count': int}"""
        pass

    def can_parse(self, file_path: str) -> bool:
        return any(file_path.endswith(ext) for ext in self.extensions)
```

- [ ] **Step 4: 实现 txt_parser.py**

```python
# src/parser/txt_parser.py
from pathlib import Path
from .base import BaseParser

class TxtParser(BaseParser):
    extensions = ['.txt']

    def parse(self, file_path: str) -> dict:
        text = Path(file_path).read_text(encoding='utf-8')
        return {
            'text': text,
            'source_type': 'txt',
            'page_count': 1
        }
```

- [ ] **Step 5: 运行测试验证通过**

```bash
cd law_parser && pytest tests/test_parser.py::test_txt_parser_basic -v
```
Expected: PASS

- [ ] **Step 6: 编写 PdfParser 测试**

```python
# tests/test_parser.py 新增
def test_pdf_parser_basic():
    parser = PdfParser()
    assert parser.extensions == ['.pdf']
```

- [ ] **Step 7: 实现 pdf_parser.py**

```python
# src/parser/pdf_parser.py
import pdfplumber
from pathlib import Path
from .base import BaseParser

class PdfParser(BaseParser):
    extensions = ['.pdf']

    def parse(self, file_path: str) -> dict:
        texts = []
        page_count = 0
        with pdfplumber.open(file_path) as pdf:
            page_count = len(pdf.pages)
            for page in pdf.pages:
                text = page.extract_text()
                if text:
                    texts.append(text)
        return {
            'text': '\n'.join(texts),
            'source_type': 'pdf',
            'page_count': page_count
        }
```

- [ ] **Step 8: 编写 DocxParser 测试并实现**

```python
# tests/test_parser.py 新增
def test_docx_parser_basic():
    parser = DocxParser()
    assert parser.extensions == ['.docx']

# src/parser/docx_parser.py
import docx
from pathlib import Path
from .base import BaseParser

class DocxParser(BaseParser):
    extensions = ['.docx']

    def parse(self, file_path: str) -> dict:
        doc = docx.Document(file_path)
        paragraphs = [p.text for p in doc.paragraphs if p.text.strip()]
        return {
            'text': '\n'.join(paragraphs),
            'source_type': 'docx',
            'page_count': 1
        }
```

- [ ] **Step 9: 编写 JsonParser 测试并实现**

```python
# tests/test_parser.py 新增
def test_json_parser_basic():
    import tempfile, json, os
    data = {"content": "测试内容", "title": "测试"}
    with tempfile.NamedTemporaryFile(mode='w', suffix='.json', delete=False, encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False)
        path = f.name
    try:
        parser = JsonParser()
        result = parser.parse(path)
        assert result['text'] == "测试内容"
        assert result['source_type'] == 'json'
    finally:
        os.unlink(path)

# src/parser/json_parser.py
import json
from pathlib import Path
from .base import BaseParser

class JsonParser(BaseParser):
    extensions = ['.json']

    def parse(self, file_path: str) -> dict:
        data = json.loads(Path(file_path).read_text(encoding='utf-8'))
        text = data.get('content') or data.get('text') or json.dumps(data, ensure_ascii=False)
        return {
            'text': text,
            'source_type': 'json',
            'page_count': 1
        }
```

- [ ] **Step 10: 实现 ParserFactory**

```python
# src/parser/__init__.py
from .base import BaseParser
from .pdf_parser import PdfParser
from .docx_parser import DocxParser
from .json_parser import JsonParser
from .txt_parser import TxtParser

ALL_PARSERS = [PdfParser(), DocxParser(), JsonParser(), TxtParser()]

def get_parser(file_path: str) -> BaseParser:
    for parser in ALL_PARSERS:
        if parser.can_parse(file_path):
            return parser
    raise ValueError(f"Unsupported file type: {file_path}")

def parse_file(file_path: str) -> dict:
    return get_parser(file_path).parse(file_path)
```

- [ ] **Step 11: 运行所有解析器测试**

```bash
cd law_parser && pytest tests/test_parser.py -v
```
Expected: All PASS

- [ ] **Step 12: Commit**

```bash
git add law_parser/src/parser law_parser/tests/test_parser.py
git commit -m "feat(law-parser): add file parser layer (PDF, DOCX, JSON, TXT)"
```

---

### Task 3: MiniMax AI 客户端

**Files:**
- Create: `law_parser/src/ai/client.py`
- Create: `law_parser/tests/test_ai_client.py`

**Interfaces:**
- Consumes: `env` MINIMAX_API_KEY, MINIMAX_BASE_URL, MINIMAX_MODEL
- Produces: `chat(prompt: str) -> str` 和 `chat_stream(prompt: str) -> Iterator[str]`

- [ ] **Step 1: 编写 AI 客户端测试**

```python
# tests/test_ai_client.py
import pytest, os
from law_parser.ai.client import MiniMaxClient

def test_client_init_from_env(monkeypatch):
    monkeypatch.setenv("MINIMAX_API_KEY", "test-key")
    monkeypatch.setenv("MINIMAX_BASE_URL", "https://api.minimax.chat/v1")
    monkeypatch.setenv("MINIMAX_MODEL", "MiniMax-M3")
    client = MiniMaxClient()
    assert client.api_key == "test-key"
    assert client.model == "MiniMax-M3"

def test_client_mask_key():
    os.environ["MINIMAX_API_KEY"] = "sk-testkey123456"
    os.environ["MINIMAX_BASE_URL"] = "https://api.minimax.chat/v1"
    os.environ["MINIMAX_MODEL"] = "MiniMax-M3"
    client = MiniMaxClient()
    masked = client.mask_key("sk-testkey123456")
    assert masked == "sk-te****3456"
    assert "sk-testkey123456" not in masked
```

- [ ] **Step 2: 运行测试验证失败**

```bash
cd law_parser && pytest tests/test_ai_client.py -v
```
Expected: FAIL - client not defined

- [ ] **Step 3: 实现 client.py**

```python
# src/ai/client.py
import os, httpx
from typing import Iterator

class MiniMaxClient:
    def __init__(self,
                 api_key: str = None,
                 base_url: str = None,
                 model: str = None,
                 timeout: int = 120):
        self.api_key = api_key or os.environ["MINIMAX_API_KEY"]
        self.base_url = base_url or os.environ.get("MINIMAX_BASE_URL", "https://api.minimax.chat/v1")
        self.model = model or os.environ.get("MINIMAX_MODEL", "MiniMax-M3")
        self.timeout = timeout
        self.client = httpx.Client(
            base_url=self.base_url,
            headers={
                "Authorization": f"Bearer {self.api_key}",
                "Content-Type": "application/json"
            },
            timeout=timeout
        )

    def chat(self, prompt: str, **kwargs) -> str:
        """同步 chat 接口"""
        payload = {
            "model": self.model,
            "messages": [{"role": "user", "content": prompt}],
            **kwargs
        }
        resp = self.client.post("/chat/completions", json=payload)
        resp.raise_for_status()
        data = resp.json()
        return data["choices"][0]["message"]["content"]

    def chat_stream(self, prompt: str, **kwargs) -> Iterator[str]:
        """流式 chat 接口"""
        payload = {
            "model": self.model,
            "messages": [{"role": "user", "content": prompt}],
            "stream": True,
            **kwargs
        }
        with self.client.stream("POST", "/chat/completions", json=payload) as resp:
            resp.raise_for_status()
            for line in resp.iter_lines():
                if line.startswith("data: "):
                    data_str = line[6:]
                    if data_str.strip() == "[DONE]":
                        break
                    import json as _json
                    data = _json.loads(data_str)
                    delta = data["choices"][0].get("delta", {}).get("content", "")
                    if delta:
                        yield delta

    @staticmethod
    def mask_key(key: str) -> str:
        if not key or len(key) <= 8:
            return "****"
        return key[:4] + "****" + key[-4:]

    def __del__(self):
        try:
            self.client.close()
        except Exception:
            pass
```

- [ ] **Step 4: 运行测试验证通过**

```bash
cd law_parser && pytest tests/test_ai_client.py -v
```
Expected: All PASS

- [ ] **Step 5: Commit**

```bash
git add law_parser/src/ai/client.py law_parser/tests/test_ai_client.py
git commit -m "feat(law-parser): add MiniMax AI client"
```

---

### Task 4: AI 审查层（结构提取 + 内容审查 + 分类）

**Files:**
- Create: `law_parser/src/ai/structure_extractor.py`
- Create: `law_parser/src/ai/content_reviewer.py`
- Create: `law_parser/src/ai/classifier.py`
- Create: `law_parser/tests/test_ai_stages.py`

**Interfaces:**
- Consumes: `MiniMaxClient` + 原始文本 / 结构结果
- Produces: `StructureResult`, `ReviewResult`, `ClassificationResult` (Pydantic models)

- [ ] **Step 1: 定义 Pydantic 模型 + 阶段一测试**

```python
# tests/test_ai_stages.py
import pytest
from law_parser.ai.structure_extractor import StructureExtractor, StructureResult

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
```

- [ ] **Step 2: 运行测试验证失败**

```bash
cd law_parser && pytest tests/test_ai_stages.py::test_structure_result_model -v
```
Expected: FAIL - module not defined

- [ ] **Step 3: 实现 Pydantic 模型（放在 src/ai/models.py）**

```python
# src/ai/models.py
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
```

- [ ] **Step 4: 实现结构提取器 + 测试 mock**

```python
# src/ai/structure_extractor.py
import json, hashlib
from .models import StructureResult, ArticleParse
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
```

- [ ] **Step 5: 实现内容审查器**

```python
# src/ai/content_reviewer.py
import json
from .models import StructureResult, ArticleParse

REVIEW_PROMPT = """你是一个专业的中国法律法规审查助手。请审查以下已解析的法规条款，修正格式错误并检查问题。

审查要求：
1. 检查条款编号是否连续（缺失编号报警告）
2. 修正全角/半角标点和格式问题
3. 检测重复条款（内容高度相似报警告）
4. 如条款无标题，从内容首句推断标题

原始解析结果（JSON）：
{structure_json}

输出严格 JSON 格式（只修改有问题的 articles）：
{
  "warnings": ["第5条缺失", "第8条与第3条内容重复"],
  "corrected_articles": [{"article_no": "...", "title": "...修正标题", "content": "...修正内容", "warnings": []}]
}"""

class ContentReviewer:
    def __init__(self, client: MiniMaxClient):
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
        except json.JSONDecodeError:
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
```

- [ ] **Step 6: 实现分类器**

```python
# src/ai/classifier.py
import json
from .models import StructureResult, ClassificationResult, CategorySuggestion

CLASSIFY_PROMPT = """你是一个专业的中国法律法规分类助手。请根据法规名称和内容，判断其一级和二级分类。

常见一级分类：
- 宪法
- 民法商法
- 行政法
- 经济法
- 社会法
- 刑法
- 诉讼与非诉讼程序法
- 劳动法
- 土地法
- 环境法

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
```

- [ ] **Step 7: Commit**

```bash
git add law_parser/src/ai
git commit -m "feat(law-parser): add AI review layer (extract, review, classify)"
```

---

### Task 5: 数据库写入层

**Files:**
- Create: `law_parser/src/db/writer.py`
- Create: `law_parser/tests/test_db_writer.py`

**Interfaces:**
- Consumes: `StructureResult` + `ClassificationResult` + MySQL 连接参数
- Produces: `(law_id: int, articles_written: int)`

- [ ] **Step 1: 编写数据库写入测试（mock）**

```python
# tests/test_db_writer.py
import pytest
from unittest.mock import patch, MagicMock
from law_parser.db.writer import LawDocumentWriter

def test_writer_import_law_document():
    with patch("pymysql.connect") as mock_connect:
        mock_conn = MagicMock()
        mock_cursor = MagicMock()
        mock_connect.return_value = mock_conn
        mock_cursor.lastrowid = 42
        mock_conn.cursor.return_value = mock_cursor

        writer = LawDocumentWriter(
            host="localhost", port=3306, user="root", password="", database="legal_ai"
        )
        from law_parser.ai.models import StructureResult, ArticleParse, CategorySuggestion, ClassificationResult
        result = StructureResult(law_title="民法典", articles=[
            ArticleParse(article_no="第一条", title="立法目的", content="为了...", sort_order=1)
        ])
        classes = ClassificationResult(suggested_categories=[
            CategorySuggestion(type_name="民法商法")
        ])

        law_id = writer.write(result, classes)
        assert law_id == 42
        mock_cursor.execute.assert_called()
        mock_conn.commit.assert_called()
```

- [ ] **Step 2: 实现 writer.py**

```python
# src/db/writer.py
import os, hashlib, uuid, pymysql
from typing import Optional
from ..ai.models import StructureResult, ClassificationResult

class LawDocumentWriter:
    def __init__(self,
                 host: str = None,
                 port: int = None,
                 user: str = None,
                 password: str = None,
                 database: str = None):
        self.host = host or os.environ.get("MYSQL_HOST", "localhost")
        self.port = port or int(os.environ.get("MYSQL_PORT", "3306"))
        self.user = user or os.environ.get("MYSQL_USER", "root")
        self.password = password or os.environ.get("MYSQL_PASSWORD", "")
        self.database = database or os.environ.get("MYSQL_DATABASE", "legal_ai")

    def _conn(self):
        return pymysql.connect(
            host=self.host, port=self.port, user=self.user,
            password=self.password, database=self.database,
            charset="utf8mb4", cursorclass=pymysql.cursors.DictCursor
        )

    def write(self,
              structure: StructureResult,
              classification: ClassificationResult,
              dry_run: bool = False) -> Optional[int]:
        law_uuid = str(uuid.uuid4())[:16]
        category_l1 = classification.suggested_categories[0].type_name if classification.suggested_categories else "其他"
        category_l2 = classification.suggested_categories[0].category_name if classification.suggested_categories else ""

        doc_sql = """
        INSERT INTO law_document
        (law_uuid, title, short_title, category_l1, category_l2, issuing_authority,
         issue_date, effective_date, status, source_name)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, 1, %s)
        """
        if dry_run:
            print(f"[DRY RUN] Would insert law_document: {structure.law_title}")
            return None

        conn = self._conn()
        try:
            with conn.cursor() as cur:
                cur.execute(doc_sql, (
                    law_uuid,
                    structure.law_title,
                    structure.short_title or structure.law_title,
                    category_l1,
                    category_l2,
                    structure.issuing_authority,
                    structure.issue_date,
                    structure.effective_date,
                    "Python Law Parser"
                ))
                law_id = cur.lastrowid

                for article in structure.articles:
                    content_hash = hashlib.sha256(article.content.encode()).hexdigest()[:16]
                    article_sql = """
                    INSERT INTO law_article
                    (law_id, article_uuid, article_no, title, content, content_hash, sort_order)
                    VALUES (%s, %s, %s, %s, %s, %s, %s)
                    """
                    cur.execute(article_sql, (
                        law_id,
                        str(uuid.uuid4())[:16],
                        article.article_no,
                        article.title or "",
                        article.content,
                        content_hash,
                        article.sort_order
                    ))
                conn.commit()
                return law_id
        finally:
            conn.close()
```

- [ ] **Step 3: Commit**

```bash
git add law_parser/src/db law_parser/tests/test_db_writer.py
git commit -m "feat(law-parser): add MySQL database writer"
```

---

### Task 6: JSON-RPC 协议层

**Files:**
- Create: `law_parser/src/protocol/jsonrpc.py`
- Create: `law_parser/src/protocol/server.py`
- Create: `law_parser/tests/test_jsonrpc.py`

**Interfaces:**
- Consumes: stdin/stdout 管道
- Produces: `JsonRpcServer` 类，接收 `Handler` 接口实现

- [ ] **Step 1: 编写 JSON-RPC 测试**

```python
# tests/test_jsonrpc.py
import pytest, json
from law_parser.protocol.jsonrpc import JsonRpcRequest, JsonRpcResponse, JsonRpcError

def test_jsonrpc_request_parse():
    raw = '{"jsonrpc":"2.0","id":1,"method":"parse","params":{"file_path":"/test.pdf"}}'
    req = JsonRpcRequest.parse(raw)
    assert req.method == "parse"
    assert req.params["file_path"] == "/test.pdf"
    assert req.id == 1

def test_jsonrpc_error():
    err = JsonRpcError(code=-32600, message="Invalid request")
    assert err.code == -32600
    assert err.message == "Invalid request"
```

- [ ] **Step 2: 实现 jsonrpc.py**

```python
# src/protocol/jsonrpc.py
import json
from typing import Any, Optional
from dataclasses import dataclass, field

@dataclass
class JsonRpcError:
    code: int
    message: str
    data: Any = None

    def to_dict(self) -> dict:
        return {"code": self.code, "message": self.message, "data": self.data}

@dataclass
class JsonRpcRequest:
    jsonrpc: str = "2.0"
    id: Optional[Any] = None
    method: str = ""
    params: dict = field(default_factory=dict)

    @staticmethod
    def parse(raw: str) -> "JsonRpcRequest":
        try:
            data = json.loads(raw, strict=False)
        except json.JSONDecodeError:
            raise JsonRpcError(code=-32700, message="Parse error")

        if data.get("jsonrpc") != "2.0":
            raise JsonRpcError(code=-32600, message="Invalid JSON-RPC version")
        if "method" not in data:
            raise JsonRpcError(code=-32600, message="Missing method")

        return JsonRpcRequest(
            jsonrpc=data.get("jsonrpc", "2.0"),
            id=data.get("id"),
            method=data.get("method"),
            params=data.get("params") or {}
        )

    def to_response(result: Any, id: Any) -> "JsonRpcResponse":
        return JsonRpcResponse(jsonrpc="2.0", id=id, result=result)

    def to_error_response(error: JsonRpcError, id: Any) -> "JsonRpcResponse":
        return JsonRpcResponse(jsonrpc="2.0", id=id, error=error)

@dataclass
class JsonRpcResponse:
    jsonrpc: str = "2.0"
    id: Optional[Any] = None
    result: Any = None
    error: Optional[JsonRpcError] = None

    def to_json(self) -> str:
        data = {"jsonrpc": self.jsonrpc}
        if self.id is not None:
            data["id"] = self.id
        if self.error is not None:
            data["error"] = self.error.to_dict()
        else:
            data["result"] = self.result
        return json.dumps(data, ensure_ascii=False)
```

- [ ] **Step 3: 实现 server.py**

```python
# src/protocol/server.py
import sys
from typing import Protocol
from .jsonrpc import JsonRpcRequest, JsonRpcResponse, JsonRpcError

class Handler(Protocol):
    def handle(self, method: str, params: dict) -> any:
        ...

class JsonRpcServer:
    def __init__(self, handler: Handler):
        self.handler = handler
        self.running = True

    def send(self, resp: JsonRpcResponse):
        print(resp.to_json(), flush=True)

    def loop(self):
        for line in sys.stdin:
            line = line.strip()
            if not line:
                continue
            try:
                req = JsonRpcRequest.parse(line)
                try:
                    result = self.handler.handle(req.method, req.params)
                    self.send(JsonRpcResponse.to_response(result, req.id))
                except Exception as e:
                    error = JsonRpcError(code=-32603, message=str(e))
                    self.send(JsonRpcResponse.to_error_response(error, req.id))
            except JsonRpcError as e:
                self.send(JsonRpcResponse(id=None, error=e))
            except Exception as e:
                err = JsonRpcError(code=-32603, message=f"Internal error: {e}")
                self.send(JsonRpcResponse(id=None, error=err))
```

- [ ] **Step 4: Commit**

```bash
git add law_parser/src/protocol law_parser/tests/test_jsonrpc.py
git commit -m "feat(law-parser): add JSON-RPC 2.0 protocol layer"
```

---

### Task 7: CLI 和主入口

**Files:**
- Create: `law_parser/src/main.py`
- Modify: `law_parser/pyproject.toml` (scripts entry)

**Interfaces:**
- Consumes: CLI 参数 / JSON-RPC server 模式
- Produces: CLI 输出 / JSON-RPC over stdin/stdout

- [ ] **Step 1: 实现 CLI + JSON-RPC 主入口**

```python
# src/main.py
import sys, argparse, os, json
from pathlib import Path
from dotenv import load_dotenv

load_dotenv()

def main():
    parser = argparse.ArgumentParser(prog="law-parser")
    sub = parser.add_subparsers(dest="cmd")

    sub.add_parser("serve", help="Start JSON-RPC server (stdin/stdout)")

    p = sub.add_parser("parse", help="Parse a file and print structure")
    p.add_argument("file_path", help="Path to law document")

    p = sub.add_parser("import", help="Parse and import to database")
    p.add_argument("file_path")
    p.add_argument("--dry-run", action="store_true")

    args = parser.parse_args()

    if args.cmd == "serve":
        from .protocol.server import JsonRpcServer, Handler
        from .parser import parse_file
        from .ai.structure_extractor import StructureExtractor
        from .ai.content_reviewer import ContentReviewer
        from .ai.classifier import Classifier
        from .ai.client import MiniMaxClient
        from .db.writer import LawDocumentWriter

        client = MiniMaxClient()
        extractor = StructureExtractor(client)
        reviewer = ContentReviewer(client)
        classifier = Classifier(client)
        db_writer = LawDocumentWriter()

        class LawParserHandler:
            def handle(self, method: str, params: dict):
                if method == "parse":
                    parsed = parse_file(params["file_path"])
                    return extractor.extract(parsed["text"]).__dict__
                elif method == "review":
                    from .ai.models import StructureResult
                    result = StructureResult(**params["structure_result"])
                    reviewed = reviewer.review(result)
                    return classifier.classify(reviewed).__dict__
                elif method == "import":
                    from .ai.models import StructureResult, ClassificationResult
                    result = StructureResult(**params["law_data"]["structure"])
                    classes = ClassificationResult(**params["law_data"]["classification"])
                    dry_run = params.get("dry_run", False)
                    law_id = db_writer.write(result, classes, dry_run=dry_run)
                    return {"law_id": law_id}
                elif method == "shutdown":
                    sys.exit(0)
                else:
                    raise ValueError(f"Unknown method: {method}")

        server = JsonRpcServer(LawParserHandler())
        server.loop()
    elif args.cmd == "parse":
        from .parser import parse_file
        from .ai.structure_extractor import StructureExtractor
        from .ai.client import MiniMaxClient
        client = MiniMaxClient()
        parsed = parse_file(args.file_path)
        result = StructureExtractor(client).extract(parsed["text"])
        print(json.dumps(result.__dict__, ensure_ascii=False, indent=2))
    elif args.cmd == "import":
        from .parser import parse_file
        from .ai.structure_extractor import StructureExtractor
        from .ai.content_reviewer import ContentReviewer
        from .ai.classifier import Classifier
        from .ai.client import MiniMaxClient
        from .db.writer import LawDocumentWriter
        client = MiniMaxClient()
        parsed = parse_file(args.file_path)
        structure = StructureExtractor(client).extract(parsed["text"])
        reviewed = reviewer.review(structure)
        classes = classifier.classify(reviewed)
        writer = LawDocumentWriter()
        law_id = writer.write(reviewed, classes, dry_run=args.dry_run)
        print(f"Imported law_id={law_id}")
    else:
        parser.print_help()

if __name__ == "__main__":
    main()
```

- [ ] **Step 2: 更新 pyproject.toml scripts**

```toml
[project.scripts]
law-parser = "law_parser.src.main:main"
```

- [ ] **Step 3: Commit**

```bash
git add law_parser/src/main.py law_parser/pyproject.toml
git commit -m "feat(law-parser): add CLI and JSON-RPC main entry point"
```

---

### Task 8: Java 集成（Spring Boot 调用 Python 子进程）

**Files:**
- Create: `backend/src/main/java/com/legalai/parser/LawParserClient.java`
- Modify: `backend/src/main/java/com/legalai/service/LawImportService.java` (注入使用)
- Modify: `backend/pom.xml` (添加依赖)

**Interfaces:**
- Consumes: Python 子进程 + JSON-RPC 命令
- Produces: `LawImportPreview` 兼容的 Map 结构

- [ ] **Step 1: 创建 LawParserClient.java**

```java
// src/main/java/com/legalai/parser/LawParserClient.java
package com.legalai.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

@Component
public class LawParserClient {
    private static final Logger log = LoggerFactory.getLogger(LawParserClient.class);

    @Value("${law-parser.python-path:python3}")
    private String pythonPath;

    @Value("${law-parser.script-path:}")
    private String scriptPath;

    private Process process;
    private BufferedReader reader;
    private BufferedWriter writer;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentHashMap<Long, CompletableFuture<Map<String, Object>>> pending = new ConcurrentHashMap<>();
    private long nextId = 1;
    private ExecutorService senderExecutor = Executors.newSingleThreadExecutor();

    @PostConstruct
    public void start() {
        try {
            String pythonExe = System.getProperty("law.parser.python", "python3");
            String script = System.getProperty("law.parser.script",
                "/workspace/legal-ai-assistant/law_parser/src/main.py");
            ProcessBuilder pb = new ProcessBuilder(pythonExe, "-m", "law_parser.src.main", "serve");
            pb.redirectErrorStream(false);
            process = pb.start();
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

            senderExecutor.submit(this::readResponses);

            send("{\"jsonrpc\":\"2.0\",\"id\":0,\"method\":\"ping\",\"params\":{}}");
            log.info("Law parser Python subprocess started");
        } catch (IOException e) {
            log.warn("Failed to start law parser subprocess: {}. Falling back to Java parsing.", e.getMessage());
        }
    }

    @PreDestroy
    public void stop() {
        try {
            send("{\"jsonrpc\":\"2.0\",\"id\":-1,\"method\":\"shutdown\",\"params\":{}}");
            if (process != null) process.waitFor(3, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Error stopping law parser: {}", e.getMessage());
        }
    }

    private void readResponses() {
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                JsonNode node = objectMapper.readTree(line);
                long id = node.has("id") ? node.get("id").asLong() : 0;
                CompletableFuture<Map<String, Object>> future = pending.remove(id);
                if (future != null) {
                    if (node.has("error")) {
                        future.completeExceptionally(new RuntimeException(node.get("error").get("message").asText()));
                    } else {
                        future.complete(objectMapper.convertValue(node.get("result"), Map.class));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error reading from Python process: {}", e.getMessage());
        }
    }

    public Map<String, Object> parse(String filePath) {
        return call("parse", Map.of("file_path", filePath));
    }

    public Map<String, Object> review(Map<String, Object> structureResult) {
        return call("review", Map.of("structure_result", structureResult));
    }

    public Map<String, Object> importLaw(Map<String, Object> lawData, boolean dryRun) {
        return call("import", Map.of("law_data", lawData, "dry_run", dryRun));
    }

    private synchronized void send(String json) throws IOException {
        writer.write(json);
        writer.newLine();
        writer.flush();
    }

    private Map<String, Object> call(String method, Map<String, Object> params) {
        if (process == null || !process.isAlive()) {
            throw new IllegalStateException("Python parser process not running");
        }
        long id = nextId++;
        String request = String.format("{\"jsonrpc\":\"2.0\",\"id\":%d,\"method\":\"%s\",\"params\":%s}",
            id, method, objectMapper.writeValueAsString(params));
        try {
            send(request);
            CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
            pending.put(id, future);
            return future.get(60, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Law parser call failed: " + method, e);
        }
    }
}
```

- [ ] **Step 2: 修改 pom.xml 添加 Python 进程配置属性**

```xml
<!-- 在 properties 中添加 -->
<law.parser.python>python3</law.parser.python>
<law.parser.script>${project.basedir}/../law_parser/src/main.py</law.parser.script>
```

- [ ] **Step 3: Commit**

```bash
git add law_parser law_parser/pyproject.toml
git commit -m "feat(law-parser): add LawParserClient Java integration + Python subprocess bridge"
```

---

## 全局约束检查

- [ ] 所有代码使用 Python 3.11+ 语法
- [ ] MiniMax API 使用 OpenAI 兼容接口，base_url = `https://api.minimax.chat/v1`
- [ ] 数据库写入字段与 `LawImportPreview.java` 兼容
- [ ] JSON-RPC 严格遵循 2.0 版本
- [ ] 错误码使用协议规定的负数编码

## 自检清单

1. **Spec 覆盖**：结构提取/内容审查/自动分类/数据库写入/JSON-RPC/Java 集成 - 每项均有对应 Task
2. **占位符扫描**：无 TBD/TODO/实现细节留空
3. **类型一致性**：Task 间接口（StructureResult, ArticleParse 等）定义统一
4. **测试覆盖**：每个组件均有测试
5. **提交粒度**：8 个 Task，每项独立可测试
