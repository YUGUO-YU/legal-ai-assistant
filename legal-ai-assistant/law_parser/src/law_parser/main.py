import sys, argparse, os, json, logging
from pathlib import Path
from dotenv import load_dotenv

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s")
load_dotenv()


def _to_serializable(obj):
    """Recursively convert Pydantic models / dataclasses to JSON-serializable dicts."""
    if hasattr(obj, "model_dump"):
        return {k: _to_serializable(v) for k, v in obj.model_dump().items()}
    if hasattr(obj, "__dict__"):
        return {k: _to_serializable(v) for k, v in obj.__dict__.items()}
    if isinstance(obj, list):
        return [_to_serializable(x) for x in obj]
    if isinstance(obj, tuple):
        return tuple(_to_serializable(x) for x in obj)
    return obj


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
        from law_parser.protocol.server import JsonRpcServer
        from law_parser.parser import parse_file
        from law_parser.ai.structure_extractor import StructureExtractor
        from law_parser.ai.content_reviewer import ContentReviewer
        from law_parser.ai.classifier import Classifier
        from law_parser.ai.client import MiniMaxClient
        from law_parser.db.writer import LawDocumentWriter

        client = MiniMaxClient()
        extractor = StructureExtractor(client)
        reviewer = ContentReviewer(client)
        classifier = Classifier(client)
        db_writer = LawDocumentWriter()

        class LawParserHandler:
            def handle(self, method: str, params: dict):
                if method == "parse":
                    parsed = parse_file(params["file_path"])
                    return _to_serializable(extractor.extract(parsed["text"]))
                elif method == "review":
                    from law_parser.ai.models import StructureResult
                    result = StructureResult(**params["structure_result"])
                    reviewed = reviewer.review(result)
                    return _to_serializable(classifier.classify(reviewed))
                elif method == "import":
                    from law_parser.ai.models import StructureResult, ClassificationResult
                    result = StructureResult(**params["law_data"]["structure"])
                    classes = ClassificationResult(**params["law_data"]["classification"])
                    dry_run = params.get("dry_run", False)
                    law_id = db_writer.write(result, classes, dry_run=dry_run)
                    return {"law_id": law_id}
                elif method == "shutdown":
                    sys.exit(0)
                elif method == "health":
                    return {"status": "ok"}
                else:
                    raise ValueError(f"Unknown method: {method}")

        server = JsonRpcServer(LawParserHandler())
        server.loop()
    elif args.cmd == "parse":
        from law_parser.parser import parse_file
        from law_parser.ai.structure_extractor import StructureExtractor
        from law_parser.ai.client import MiniMaxClient
        client = MiniMaxClient()
        parsed = parse_file(args.file_path)
        result = StructureExtractor(client).extract(parsed["text"])
        print(json.dumps(_to_serializable(result), ensure_ascii=False, indent=2))
    elif args.cmd == "import":
        from law_parser.parser import parse_file
        from law_parser.ai.structure_extractor import StructureExtractor
        from law_parser.ai.content_reviewer import ContentReviewer
        from law_parser.ai.classifier import Classifier
        from law_parser.ai.client import MiniMaxClient
        from law_parser.db.writer import LawDocumentWriter
        client = MiniMaxClient()
        parsed = parse_file(args.file_path)
        structure = StructureExtractor(client).extract(parsed["text"])
        reviewed = ContentReviewer(client).review(structure)
        classes = Classifier(client).classify(reviewed)
        writer = LawDocumentWriter()
        law_id = writer.write(reviewed, classes, dry_run=args.dry_run)
        print(f"Imported law_id={law_id}")
    else:
        parser.print_help()

if __name__ == "__main__":
    main()
