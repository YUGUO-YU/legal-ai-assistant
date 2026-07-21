import json
from typing import Any, Optional
from dataclasses import dataclass, field

@dataclass
class JsonRpcError:
    code: int
    message: str
    data: Any = None

    def to_dict(self) -> dict:
        return {"code": self.code, "message": self.message, "data": self.data}

@dataclass
class JsonRpcRequest:
    jsonrpc: str = "2.0"
    id: Optional[Any] = None
    method: str = ""
    params: dict = field(default_factory=dict)

    @staticmethod
    def parse(raw: str) -> "JsonRpcRequest":
        try:
            data = json.loads(raw, strict=False)
        except json.JSONDecodeError:
            raise JsonRpcError(code=-32700, message="Parse error")

        if data.get("jsonrpc") != "2.0":
            raise JsonRpcError(code=-32600, message="Invalid JSON-RPC version")
        if "method" not in data:
            raise JsonRpcError(code=-32600, message="Missing method")

        return JsonRpcRequest(
            jsonrpc=data.get("jsonrpc", "2.0"),
            id=data.get("id"),
            method=data.get("method"),
            params=data.get("params") or {}
        )

    def to_response(result: Any, id: Any) -> "JsonRpcResponse":
        return JsonRpcResponse(jsonrpc="2.0", id=id, result=result)

    def to_error_response(error: JsonRpcError, id: Any) -> "JsonRpcResponse":
        return JsonRpcResponse(jsonrpc="2.0", id=id, error=error)

@dataclass
class JsonRpcResponse:
    jsonrpc: str = "2.0"
    id: Optional[Any] = None
    result: Any = None
    error: Optional[JsonRpcError] = None

    def to_json(self) -> str:
        data = {"jsonrpc": self.jsonrpc}
        if self.id is not None:
            data["id"] = self.id
        if self.error is not None:
            data["error"] = self.error.to_dict()
        else:
            data["result"] = self.result
        return json.dumps(data, ensure_ascii=False)