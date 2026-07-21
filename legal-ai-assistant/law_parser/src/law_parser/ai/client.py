import os, httpx
from typing import Iterator

class MiniMaxClient:
    def __init__(self,
                 api_key: str = None,
                 base_url: str = None,
                 model: str = None,
                 timeout: int = 120):
        self.api_key = api_key or os.environ["MINIMAX_API_KEY"]
        self.base_url = base_url or os.environ.get("MINIMAX_BASE_URL", "https://api.minimax.chat/v1")
        self.model = model or os.environ.get("MINIMAX_MODEL", "MiniMax-M3")
        self.timeout = timeout
        self.client = httpx.Client(
            base_url=self.base_url,
            headers={
                "Authorization": f"Bearer {self.api_key}",
                "Content-Type": "application/json"
            },
            timeout=timeout
        )

    def chat(self, prompt: str, **kwargs) -> str:
        """同步 chat 接口"""
        payload = {
            "model": self.model,
            "messages": [{"role": "user", "content": prompt}],
            **kwargs
        }
        resp = self.client.post("/chat/completions", json=payload)
        resp.raise_for_status()
        data = resp.json()
        return data["choices"][0]["message"]["content"]

    def chat_stream(self, prompt: str, **kwargs) -> Iterator[str]:
        """流式 chat 接口"""
        payload = {
            "model": self.model,
            "messages": [{"role": "user", "content": prompt}],
            "stream": True,
            **kwargs
        }
        with self.client.stream("POST", "/chat/completions", json=payload) as resp:
            resp.raise_for_status()
            for line in resp.iter_lines():
                if line.startswith("data: "):
                    data_str = line[6:]
                    if data_str.strip() == "[DONE]":
                        break
                    import json as _json
                    data = _json.loads(data_str)
                    delta = data["choices"][0].get("delta", {}).get("content", "")
                    if delta:
                        yield delta

    @staticmethod
    def mask_key(key: str) -> str:
        if not key or len(key) <= 8:
            return "****"
        return key[:5] + "****" + key[-4:]

    def __del__(self):
        try:
            self.client.close()
        except Exception:
            pass