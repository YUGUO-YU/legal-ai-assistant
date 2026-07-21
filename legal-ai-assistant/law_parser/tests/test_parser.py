import pytest, tempfile, os, json
from law_parser.parser.txt_parser import TxtParser
from law_parser.parser.pdf_parser import PdfParser
from law_parser.parser.docx_parser import DocxParser
from law_parser.parser.json_parser import JsonParser

def test_txt_parser_basic():
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

def test_pdf_parser_basic():
    parser = PdfParser()
    assert parser.extensions == ['.pdf']

def test_docx_parser_basic():
    parser = DocxParser()
    assert parser.extensions == ['.docx']

def test_json_parser_basic():
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