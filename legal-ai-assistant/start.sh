#!/bin/bash
# Legal AI Assistant 一键启动脚本
# 后端直接接入 MiniMax-M3 (OpenAI 兼容协议)。

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "=== 启动 Legal AI Assistant ==="
echo "AI 接入: MiniMax-M3 (OpenAI 兼容协议)"
echo "API Key 读取: 环境变量 MINIMAX_API_KEY"

# 0. 检查 MiniMax API Key 是否配置
if [ -z "$MINIMAX_API_KEY" ]; then
    if [ -f "$SCRIPT_DIR/backend/.env" ]; then
        export $(grep -v '^#' "$SCRIPT_DIR/backend/.env" | xargs)
    fi
fi
if [ -z "$MINIMAX_API_KEY" ]; then
    echo "警告: 未设置 MINIMAX_API_KEY，请配置后重启，否则 AI 调用会失败"
fi

# 1. 启动后端
echo "[1/2] 启动后端服务..."
cd "$SCRIPT_DIR/backend"
nohup ./mvnw spring-boot:run -DskipTests > /tmp/backend.log 2>&1 &
BACKEND_PID=$!
echo "后端进程已启动，PID: $BACKEND_PID"

echo "等待后端服务启动..."
for i in {1..90}; do
    if curl -s -m 2 http://localhost:3001/api/v1/health > /dev/null 2>&1; then
        echo "后端服务已就绪"
        break
    fi
    if [ $i -eq 90 ]; then
        echo "警告: 后端服务启动超时，继续启动前端..."
    fi
    sleep 1
done

# 2. 启动前端
echo "[2/2] 启动前端服务..."
cd "$SCRIPT_DIR/frontend"

if [ ! -d "node_modules" ]; then
    echo "安装前端依赖..."
    npm install
fi

nohup npm run dev > /tmp/frontend.log 2>&1 &
FRONTEND_PID=$!
echo "前端进程已启动，PID: $FRONTEND_PID"

echo "=== 启动完成 ==="
echo "后端API:  http://localhost:3001"
echo "AI 状态:  http://localhost:3001/api/v1/ai-status"
echo "前端:     请查看 /tmp/frontend.log 中的访问地址"
echo "后端日志: /tmp/backend.log"
echo "前端日志: /tmp/frontend.log"
