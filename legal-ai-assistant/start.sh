#!/bin/bash
# Legal AI Assistant 一键启动脚本

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "=== 启动 Legal AI Assistant ==="

# 1. 启动 OpenClaw Gateway
echo "[1/3] 启动 OpenClaw Gateway..."
pkill -f openclaw 2>/dev/null || true
nohup openclaw gateway > /tmp/openclaw.log 2>&1 &

# 等待 OpenClaw 就绪
echo "等待 OpenClaw 启动..."
for i in {1..30}; do
    if curl -s -m 2 http://localhost:19001/v1/models -H "Authorization: Bearer my-secret-token" > /dev/null 2>&1; then
        echo "OpenClaw Gateway 已就绪"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "警告: OpenClaw 启动超时，继续..."
    fi
    sleep 1
done

# 2. 启动后端
echo "[2/3] 启动后端服务..."
cd "$SCRIPT_DIR/backend"
nohup ./mvnw spring-boot:run -DskipTests > /tmp/backend.log 2>&1 &

# 等待后端就绪
echo "等待后端服务启动..."
for i in {1..90}; do
    if curl -s -m 2 http://localhost:3001/api/v1/health > /dev/null 2>&1; then
        echo "后端服务已就绪"
        break
    fi
    if [ $i -eq 90 ]; then
        echo "警告: 后端服务启动超时，继续..."
    fi
    sleep 1
done

# 3. 启动前端
echo "[3/3] 启动前端服务..."
cd "$SCRIPT_DIR/frontend"

# 检查是否需要安装依赖
if [ ! -d "node_modules" ]; then
    echo "安装前端依赖..."
    npm install
fi

nohup npm run dev > /tmp/frontend.log 2>&1 &

echo "=== 启动完成 ==="
echo "OpenClaw: http://localhost:19001"
echo "后端API:  http://localhost:3001"
echo "前端:     请查看 /tmp/frontend.log 中的访问地址"