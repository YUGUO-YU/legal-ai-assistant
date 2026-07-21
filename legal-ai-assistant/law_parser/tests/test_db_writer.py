import pytest
from unittest.mock import patch, MagicMock
from law_parser.db.writer import LawDocumentWriter

def test_writer_import_law_document():
    with patch("pymysql.connect") as mock_connect:
        mock_conn = MagicMock()
        mock_cursor = MagicMock()
        mock_connect.return_value = mock_conn
        mock_cursor.lastrowid = 42
        mock_cursor.__enter__ = MagicMock(return_value=mock_cursor)
        mock_cursor.__exit__ = MagicMock(return_value=None)
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