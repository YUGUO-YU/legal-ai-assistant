import pytest, json
from law_parser.protocol.jsonrpc import JsonRpcRequest, JsonRpcResponse, JsonRpcError

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