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