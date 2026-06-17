#!/bin/bash
# Legal AI Assistant 一键启动脚本

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

OPENCLAW_TOKEN="my-secret-token"
OPENCLAW_CHECK_INTERVAL=30
MAX_RESTART_ATTEMPTS=3

echo "=== 启动 Legal AI Assistant ==="

# 启动 OpenClaw Gateway
start_openclaw() {
    echo "启动 OpenClaw Gateway..."
    pkill -f openclaw 2>/dev/null || true
    nohup openclaw gateway > /tmp/openclaw.log 2>&1 &
    echo "OpenClaw 进程已启动，PID: $!"
}

# 检查 OpenClaw 是否就绪
check_openclaw() {
    curl -s -m 3 http://localhost:19001/v1/models -H "Authorization: Bearer $OPENCLAW_TOKEN" > /dev/null 2>&1
    return $?
}

# 等待 OpenClaw 就绪
wait_for_openclaw() {
    echo "等待 OpenClaw 启动..."
    for i in {1..60}; do
        if check_openclaw; then
            echo "OpenClaw Gateway 已就绪"
            return 0
        fi
        sleep 1
    done
    return 1
}

# 1. 启动 OpenClaw Gateway
start_openclaw
if ! wait_for_openclaw; then
    echo "警告: OpenClaw 启动超时，尝试重启..."
    pkill -f openclaw 2>/dev/null || true
    sleep 2
    start_openclaw
    if ! wait_for_openclaw; then
        echo "错误: OpenClaw 启动失败，请检查日志 /tmp/openclaw.log"
        exit 1
    fi
fi

# 启动 OpenClaw 健康检查守护进程
start_openclaw_monitor() {
    echo "启动 OpenClaw 健康检查守护进程..."
    (
        restart_count=0
        while true; do
            sleep $OPENCLAW_CHECK_INTERVAL
            if ! check_openclaw; then
                echo "[$(date)] OpenClaw 连接失败，尝试重启..." >> /tmp/openclaw_monitor.log
                pkill -f openclaw 2>/dev/null || true
                sleep 2
                start_openclaw
                if wait_for_openclaw; then
                    echo "[$(date)] OpenClaw 重启成功" >> /tmp/openclaw_monitor.log
                    restart_count=0
                else
                    restart_count=$((restart_count + 1))
                    echo "[$(date)] OpenClaw 重启失败 (尝试 $restart_count/$MAX_RESTART_ATTEMPTS)" >> /tmp/openclaw_monitor.log
                    if [ $restart_count -ge $MAX_RESTART_ATTEMPTS ]; then
                        echo "[$(date)] 达到最大重启次数，停止监控" >> /tmp/openclaw_monitor.log
                        break
                    fi
                fi
            fi
        done
    ) &
    echo "OpenClaw 健康检查守护进程已启动，PID: $!"
}

start_openclaw_monitor

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
echo "OpenClaw 监控日志: /tmp/openclaw_monitor.log"