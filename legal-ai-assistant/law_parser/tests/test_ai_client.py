import pytest, os, time
from unittest.mock import patch, MagicMock
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

def test_client_retry_on_failure(monkeypatch):
    import httpx
    monkeypatch.setenv("MINIMAX_API_KEY", "test-key")
    monkeypatch.setenv("MINIMAX_BASE_URL", "https://api.minimax.chat/v1")
    monkeypatch.setenv("MINIMAX_MODEL", "MiniMax-M3")
    client = MiniMaxClient()
    call_count = 0
    def mock_post(*args, **kwargs):
        nonlocal call_count
        call_count += 1
        if call_count <= 2:
            raise httpx.RequestError("Transient error")
        mock_resp = MagicMock()
        mock_resp.raise_for_status = MagicMock()
        mock_resp.json = MagicMock(return_value={"choices": [{"message": {"content": "success"}}]})
        return mock_resp
    with patch.object(client.client, 'post', side_effect=mock_post):
        result = client.chat("test prompt")
        assert result == "success"
        assert call_count == 3

def test_client_ai_client_max_retries():
    assert MiniMaxClient.MAX_RETRIES == 2
    assert MiniMaxClient.RETRY_DELAY == 1.0