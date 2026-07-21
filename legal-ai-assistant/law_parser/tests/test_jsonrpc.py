import pytest, json
from law_parser.protocol.jsonrpc import JsonRpcRequest, JsonRpcResponse, JsonRpcError
from law_parser.protocol.server import JsonRpcServer

class DummyHandler:
    def handle(self, method, params):
        if method == "parse":
            return {"text": f"parsed {params.get('file_path', '')}"}
        elif method == "health":
            return {"status": "ok"}
        raise ValueError(f"Unknown method: {method}")

def test_jsonrpc_request_parse():
    raw = '{"jsonrpc":"2.0","id":1,"method":"parse","params":{"file_path":"/test.pdf"}}'
    req = JsonRpcRequest.parse(raw)
    assert req.method == "parse"
    assert req.params["file_path"] == "/test.pdf"
    assert req.id == 1

def test_jsonrpc_error():
    err = JsonRpcError(code=-32600, message="Invalid request")
    assert err.code == -32600
    assert err.message == "Invalid request"

def test_jsonrpc_response_to_json():
    resp = JsonRpcResponse(id=1, result={"law_id": 42})
    data = json.loads(resp.to_json())
    assert data["result"]["law_id"] == 42
    assert data["id"] == 1

def test_jsonrpc_server_handler_response():
    handler = DummyHandler()
    server = JsonRpcServer(handler)
    req = {"jsonrpc": "2.0", "id": 1, "method": "health", "params": {}}
    parsed = JsonRpcRequest.parse(json.dumps(req))
    assert parsed.method == "health"
    assert parsed.id == 1