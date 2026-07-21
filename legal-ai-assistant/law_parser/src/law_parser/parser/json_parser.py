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