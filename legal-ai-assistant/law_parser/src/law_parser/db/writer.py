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