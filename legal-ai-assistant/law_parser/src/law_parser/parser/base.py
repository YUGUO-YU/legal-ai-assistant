from abc import ABC, abstractmethod

class BaseParser(ABC):
    extensions: list[str] = []

    @abstractmethod
    def parse(self, file_path: str) -> dict:
        """返回 {'text': str, 'source_type': str, 'page_count': int}"""
        pass

    def can_parse(self, file_path: str) -> bool:
        return any(file_path.endswith(ext) for ext in self.extensions)