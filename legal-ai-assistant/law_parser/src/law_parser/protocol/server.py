import sys
from typing import Protocol
from .jsonrpc import JsonRpcRequest, JsonRpcResponse, JsonRpcError

class Handler(Protocol):
    def handle(self, method: str, params: dict) -> any:
        ...

class JsonRpcServer:
    def __init__(self, handler: Handler):
        self.handler = handler
        self.running = True

    def send(self, resp: JsonRpcResponse):
        print(resp.to_json(), flush=True)

    def loop(self):
        for line in sys.stdin:
            line = line.strip()
            if not line:
                continue
            try:
                req = JsonRpcRequest.parse(line)
                try:
                    result = self.handler.handle(req.method, req.params)
                    self.send(JsonRpcResponse.to_response(result, req.id))
                except Exception as e:
                    error = JsonRpcError(code=-32603, message=str(e))
                    self.send(JsonRpcResponse.to_error_response(error, req.id))
            except JsonRpcError as e:
                self.send(JsonRpcResponse(id=None, error=e))
            except Exception as e:
                err = JsonRpcError(code=-32603, message=f"Internal error: {e}")
                self.send(JsonRpcResponse(id=None, error=err))