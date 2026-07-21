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