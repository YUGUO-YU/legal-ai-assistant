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