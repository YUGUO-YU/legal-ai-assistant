# OpenClaw 配置说明

> **部署位置**（Windows）：所有文件统一放到 `E:\legal-ai-assistant\legal-ai-assistant\openclaw` 下，不要再放到 `E:\openclaw`。详细操作见 `openclaw/README.md`。

## 安装信息
- OpenClaw 版本: 2026.6.6
- 二进制路径（Windows）: `E:\legal-ai-assistant\legal-ai-assistant\openclaw\openclaw.exe`
- 配置文件（Windows）: `E:\legal-ai-assistant\legal-ai-assistant\openclaw\openclaw.json`
- 启动日志（Spring Boot 重定向）: `E:\legal-ai-assistant\legal-ai-assistant\backend\logs\openclaw_stderr.log`

> Linux/macOS: 二进制 `/usr/local/bin/openclaw`，配置 `~/.openclaw/openclaw.json`，日志 `/tmp/openclaw_stderr.log`。

## MiniMax API 配置 (M3)
```json
{
  "env": {
    "MINIMAX_API_KEY": "sk-cp-ekoYcsSAxZvJrF7fvTa6Ysotr5tUdq0tuDip1T288mxFO2VoH6oguSDnUvHUyyrpY1BzzQeifVkOTrSZMZ_gnr1NrvimwFs7IWccNtjjqMyLq1wOEBuagUs"
  },
  "agents": {
    "defaults": {
      "model": "minimax/MiniMax-M3"
    }
  },
  "models": {
    "mode": "merge",
    "providers": {
      "minimax": {
        "baseUrl": "https://api.minimax.chat/v1",
        "apiKey": "${MINIMAX_API_KEY}",
        "api": "openai-completions",
        "models": [
          {
            "id": "MiniMax-M3",
            "name": "MiniMax M3",
            "input": ["text"],
            "contextWindow": 1000000,
            "maxTokens": 163840
          }
        ]
      }
    }
  },
  "gateway": {
    "mode": "local",
    "port": 19001,
    "auth": {
      "mode": "token",
      "token": "my-secret-token"
    },
    "http": {
      "endpoints": {
        "chatCompletions": { "enabled": true }
      }
    }
  }
}
```

## 启动网关
```bash
nohup openclaw gateway --allow-unconfigured > /tmp/openclaw-gateway.log 2>&1 &
```

## 验证配置
```bash
openclaw models list | grep minimax
openclaw config validate
```

## 网关健康检查
```bash
curl http://localhost:19001/health
```

## API调用示例
```bash
curl -s -X POST http://localhost:19001/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer my-secret-token" \
  -d '{
    "model": "openclaw",
    "messages": [{"role":"user","content":"say hi"}]
  }'
```

## 模型信息
| 模型 | 上下文窗口 | 最大输出 |
|------|-----------|---------|
| MiniMax-M3 | 1,000,000 tokens | 163,840 tokens |
| MiniMax-M2.7 | 200,000 tokens | 131,072 tokens |
| MiniMax-M2 | 200,000 tokens | 131,072 tokens |
