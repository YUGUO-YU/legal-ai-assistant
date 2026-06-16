# OpenClaw 配置说明

## 安装信息
- OpenClaw 版本: 2026.6.6
- 安装路径: /usr/local/bin/openclaw
- 配置目录: ~/.openclaw/

## 配置文件
- 配置文件: ~/.openclaw/openclaw.json
- 日志文件: /tmp/openclaw/openclaw-*.log

## MiniMax API 配置
```json
{
  "env": {
    "MINIMAX_API_KEY": "sk-cp-ekoYcsSAxZvJrF7fvTa6Ysotr5tUdq0tuDip1T288mxFO2VoH6oguSDnUvHUyyrpY1BzzQeifVkOTrSZMZ_gnr1NrvimwFs7IWccNtjjqMyLq1wOEBuagUs"
  },
  "models": {
    "providers": {
      "minimax": {
        "baseUrl": "https://api.minimax.io/anthropic",
        "apiKey": "${MINIMAX_API_KEY}",
        "api": "anthropic-messages"
      }
    }
  }
}
```

## 启动网关
```bash
openclaw gateway --allow-unconfigured
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
