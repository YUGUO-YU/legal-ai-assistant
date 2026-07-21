import pytest, os
from law_parser.ai.client import MiniMaxClient

def test_client_init_from_env(monkeypatch):
    monkeypatch.setenv("MINIMAX_API_KEY", "test-key")
    monkeypatch.setenv("MINIMAX_BASE_URL", "https://api.minimax.chat/v1")
    monkeypatch.setenv("MINIMAX_MODEL", "MiniMax-M3")
    client = MiniMaxClient()
    assert client.api_key == "test-key"
    assert client.model == "MiniMax-M3"

def test_client_mask_key():
    os.environ["MINIMAX_API_KEY"] = "sk-testkey123456"
    os.environ["MINIMAX_BASE_URL"] = "https://api.minimax.chat/v1"
    os.environ["MINIMAX_MODEL"] = "MiniMax-M3"
    client = MiniMaxClient()
    masked = client.mask_key("sk-testkey123456")
    assert masked == "sk-te****3456"
    assert "sk-testkey123456" not in masked