# OpenClaw 部署说明

本目录统一存放 OpenClaw 网关所有文件，**不要**再把 `openclaw.exe` 装到 `E:\openclaw` 等其他位置。

## 目录结构

```
E:\legal-ai-assistant\legal-ai-assistant\
├── openclaw\
│   ├── openclaw.exe               # OpenClaw 可执行文件（请自行放置）
│   ├── openclaw.json              # OpenClaw 配置文件（请从 openclaw.example.json 复制并填入 API Key）
│   └── openclaw.example.json      # 配置文件模板
└── backend\
    └── logs\
        └── openclaw_stderr.log    # Spring Boot 启动 OpenClaw 时的输出日志
```

## 一次性安装步骤

1. 把 `openclaw.exe` 放到本目录：

   ```bat
   copy E:\Downloads\openclaw.exe E:\legal-ai-assistant\legal-ai-assistant\openclaw\openclaw.exe
   ```

2. 复制配置模板并填入真实 API Key：

   ```bat
   cd E:\legal-ai-assistant\legal-ai-assistant
   copy openclaw\openclaw.example.json openclaw\openclaw.json
   notepad openclaw\openclaw.json
   ```

   把 `<YOUR_MINIMAX_API_KEY>` 替换为 `openclaw-config.md` 中的真实 key。

3. 启动后端：

   ```bat
   cd E:\legal-ai-assistant\legal-ai-assistant
   mvn -q -f backend\pom.xml clean package -DskipTests
   java -jar backend\target\legal-ai-assistant-*.jar
   ```

   启动日志应出现：

   ```
   使用 openclaw 二进制: E:\legal-ai-assistant\legal-ai-assistant\openclaw\openclaw.exe
   使用配置文件: E:\legal-ai-assistant\legal-ai-assistant\openclaw\openclaw.json
   OpenClaw 日志文件: E:\legal-ai-assistant\legal-ai-assistant\backend\logs\openclaw_stderr.log
   OpenClaw Gateway 自动启动成功 (Ns)
   ```

## 配置项说明

`backend/src/main/resources/application.yml` 中 `ai.openclaw.*` 字段：

| 配置项 | 作用 |
|--------|------|
| `binary-path` | openclaw 可执行文件绝对路径 |
| `config-path` | openclaw 配置文件绝对路径，会通过 `OPENCLAW_CONFIG` 环境变量传入 |
| `log-path` | Spring Boot 启动 openclaw 时的 stdout/stderr 重定向文件 |
| `gateway-args` | openclaw 启动参数（`gateway --allow-unconfigured`） |
| `url` / `token` | 业务侧调用网关的地址与令牌 |
