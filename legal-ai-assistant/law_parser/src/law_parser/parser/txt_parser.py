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