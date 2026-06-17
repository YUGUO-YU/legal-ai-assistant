# 法律AI助手系统 - 部署指南

本文档详细介绍如何在本地环境部署法律AI助手系统。

## 目录

- [环境要求](#环境要求)
- [快速部署（Mock模式）](#快速部署mock模式)
- [生产环境部署](#生产环境部署)
- [数据库配置](#数据库配置)
- [启动项目](#启动项目)
- [Docker部署](#docker部署)
- [常见问题](#常见问题)

---

## 环境要求

### 必需环境

| 软件 | 版本 | 说明 |
|------|------|------|
| Node.js | 18+ | 前端构建和开发服务器 |
| JDK | 17+ | 后端运行环境 |
| Maven | 3.8+ | 后端项目构建 |

### 可选环境（生产环境需要）

| 软件 | 版本 | 说明 |
|------|------|------|
| MySQL | 8.0+ | 主数据库 |
| Redis | 7.x | 缓存（可选） |
| Elasticsearch | 8.x | 全文检索（可选） |
| Milvus | 2.4+ | 向量数据库（可选） |

### 验证环境

```bash
# Node.js
node -v    # v18.x.x 或更高

# JDK
java -version    # openjdk 17.x.x

# Maven
mvn -version    # Apache Maven 3.8.x 或更高
```

---

## 快速部署（Mock模式）

Mock模式使用模拟数据，无需配置MySQL、Redis等外部服务，适合演示和开发测试。

### 步骤1：克隆代码

```bash
git clone https://github.com/YUGUO-YU/legal-ai-assistant.git
cd legal-ai-assistant
```

### 步骤2：启动后端

```bash
cd backend

# 方式一：直接运行
mvn spring-boot:run

# 方式二：打包后运行
mvn clean package -DskipTests
java -jar target/legal-ai-assistant-1.0.0.jar
```

后端启动后运行在 http://localhost:3001

### 步骤3：启动前端

```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

前端启动后运行在 http://localhost:5173

### 步骤4：访问应用

1. 打开浏览器访问 http://localhost:5173
2. 使用任意用户名和密码（长度>=6位）登录

---

## 生产环境部署

### 步骤1：安装 MySQL 8.0

#### Windows

1. 下载 MySQL Installer: https://dev.mysql.com/downloads/installer/

2. 运行安装程序，选择「Custom」安装类型

3. 选择安装：
   - MySQL Server 8.0.x
   - MySQL Workbench（可选）

4. 配置实例：
   - Port: 3306
   - Root密码: 设置一个强密码（记住此密码）

5. 完成安装

#### Linux (Ubuntu/Debian)

```bash
sudo apt update
sudo apt install mysql-server
sudo systemctl start mysql
sudo systemctl enable mysql

# 安全配置
sudo mysql_secure_installation
```

#### macOS

```bash
brew install mysql
brew services start mysql
```

### 步骤2：创建数据库

```bash
# 登录MySQL
mysql -u root -p

# 创建数据库
CREATE DATABASE legal_ai CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 验证
SHOW DATABASES;

# 退出
EXIT;
```

### 步骤3：导入数据库 schema

```bash
cd legal-ai-assistant/backend

# 导入schema
mysql -u root -p legal_ai < src/main/resources/schema.sql

# 或使用MySQL命令行
mysql -u root -p
USE legal_ai;
SOURCE src/main/resources/schema.sql;
```

### 步骤4：配置后端

编辑 `backend/src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: legal-ai-assistant

  datasource:
    url: jdbc:mysql://localhost:3306/legal_ai?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: your_mysql_password
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000

  sql:
    init:
      mode: always
      schema-locations: classpath:schema.sql
      continue-on-error: true

  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
      - org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration

server:
  port: 3001

ai:
  openclaw:
    url: http://localhost:19001
    token: your_openclaw_token
  minimax:
    model: openclaw
    timeout: 120

# Mock模式：生产环境设为false
mock:
  enabled: false

# Elasticsearch（可选）
elasticsearch:
  host: localhost
  port: 9200
  index-name: legal_law_articles
  enabled: false

# Milvus向量数据库（可选）
milvus:
  host: localhost
  port: 19530
  collection-name: legal_law_articles
  dimension: 1536
  enabled: false
```

### 步骤5：配置 OpenClaw/MiniMax AI

编辑 `~/.openclaw/openclaw.json`:

```json
{
  "openclaw": {
    "url": "http://localhost:19001",
    "token": "your-token-here"
  },
  "model": "openclaw"
}
```

或在 `application.yml` 中配置：

```yaml
ai:
  openclaw:
    url: http://localhost:19001
    token: your_openclaw_token
```

### 步骤6：构建并启动后端

```bash
cd backend

# 清理并打包（跳过测试）
mvn clean package -DskipTests

# 后台运行
nohup java -jar target/legal-ai-assistant-1.0.0.jar > app.log 2>&1 &

# 验证启动
curl http://localhost:3001/api/v1/health
```

### 步骤7：构建前端

```bash
cd frontend

# 安装依赖
npm install

# 构建生产版本
npm run build
```

构建产物在 `frontend/dist` 目录

### 步骤8：配置 Nginx

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 前端静态文件
    location / {
        root /path/to/legal-ai-assistant/frontend/dist;
        try_files $uri $uri/ /index.html;
    }

    # API代理
    location /api {
        proxy_pass http://localhost:3001;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # SSE流式响应
    location /api {
        proxy_set_header Connection '';
        proxy_http_version 1.1;
        chunked_transfer_encoding on;
        proxy_buffering off;
        proxy_cache off;
    }
}
```

---

## 数据库配置

### schema.sql 表结构

系统包含以下数据表：

| 表名 | 说明 |
|------|------|
| kb_knowledge_base | 知识库表 |
| kb_document | 文档表 |
| kb_chat_message | 聊天消息表 |
| law_document | 法规表 |
| law_article | 法规条款表 |
| law_category | 法规分类表 |

### 初始化数据

`schema.sql` 包含示例法规数据：

```sql
-- 初始化示例法规
INSERT INTO law_document (...) VALUES
('LAW-2023-001', '中华人民共和国民法典', ...),
('LAW-2023-002', '中华人民共和国劳动合同法', ...),
('LAW-2023-003', '中华人民共和国公司法', ...);
```

### 数据迁移

如需迁移数据到生产环境：

```bash
# 导出数据
mysqldump -u root -p legal_ai > backup.sql

# 导入数据
mysql -u root -p legal_ai < backup.sql
```

---

## 启动项目

### 开发模式启动

#### 后端开发服务器

```bash
cd backend

# 启用热重载
mvn spring-boot:run -Dspring-boot.run.fork=false

# 或指定配置文件
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

#### 前端开发服务器

```bash
cd frontend

# 启动开发服务器（支持热重载）
npm run dev

# 访问 http://localhost:5173
```

### 生产模式启动

#### 后端服务

```bash
cd backend

# 构建
mvn clean package -DskipTests

# 运行
java -jar target/legal-ai-assistant-1.0.0.jar

# 或指定端口
java -jar target/legal-ai-assistant-1.0.0.jar --server.port=8080
```

#### 使用 systemd 服务（Linux）

创建 `/etc/systemd/system/legal-ai.service`:

```ini
[Unit]
Description=Legal AI Assistant
After=network.target mysql.service

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/path/to/legal-ai-assistant/backend
ExecStart=/usr/bin/java -jar target/legal-ai-assistant-1.0.0.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

管理服务：

```bash
sudo systemctl daemon-reload
sudo systemctl enable legal-ai
sudo systemctl start legal-ai
sudo systemctl status legal-ai
```

---

## Docker部署

### Dockerfile (后端)

```dockerfile
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY backend/target/legal-ai-assistant-1.0.0.jar app.jar
EXPOSE 3001
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Docker Compose

创建 `docker-compose.yml`:

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: your_password
      MYSQL_DATABASE: legal_ai
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./backend/src/main/resources/schema.sql:/docker-entrypoint-initdb.d/schema.sql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]

  backend:
    build: ./backend
    ports:
      - "3001:3001"
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/legal_ai
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: your_password
      MOCK_ENABLED: false
    depends_on:
      mysql:
        condition: service_healthy

  frontend:
    build: ./frontend
    ports:
      - "80:80"
    depends_on:
      - backend

volumes:
  mysql_data:
```

启动：

```bash
docker-compose up -d
docker-compose logs -f
```

停止：

```bash
docker-compose down
```

---

## 常见问题

### 1. Maven 构建失败

检查环境变量：

```bash
# Windows
echo %MAVEN_HOME%
echo %JAVA_HOME%

# Linux/macOS
echo $MAVEN_HOME
echo $JAVA_HOME
```

清理后重试：

```bash
mvn clean
mvn clean package -DskipTests
```

### 2. 端口被占用

```bash
# Windows: 查看端口占用
netstat -ano | findstr :3001

# Linux: 查看端口占用
lsof -i :3001

# 结束占用进程
# Windows
taskkill /PID <PID> /F

# Linux
kill -9 <PID>
```

或修改端口：

```bash
java -jar app.jar --server.port=8080
```

### 3. MySQL 连接失败

```bash
# 检查MySQL服务状态
# Windows
net start mysql

# Linux
sudo systemctl status mysql

# 测试连接
mysql -u root -p -h localhost
```

### 4. 前端无法连接后端

检查后端 CORS 配置：

```yaml
# application.yml
server:
  port: 3001
```

前端 API 配置在 `frontend/src/api/index.js`:

```javascript
const api = axios.create({
  baseURL: '/api/v1',  // 确保代理配置正确
  timeout: 30000
})
```

### 5. Mock模式与真实AI切换

```yaml
# application.yml
mock:
  enabled: true   # 使用模拟数据
  # enabled: false  # 使用真实OpenClaw API
```

### 6. 内存不足

```bash
# 增加JVM内存
java -Xmx2g -jar app.jar

# 或在pom.xml中配置
<arguments>
  <argument>-Xmx2048m</argument>
</arguments>
```

### 7. 日志查看

```bash
# 后端日志
tail -f logs/spring.log

# Docker日志
docker-compose logs -f backend

# Systemd日志
journalctl -u legal-ai -f
```

---

## 验证部署

### 后端健康检查

```bash
curl http://localhost:3001/api/v1/health
```

### API 登录测试

```bash
curl -X POST http://localhost:3001/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"123456"}'
```

### 前端访问测试

打开浏览器访问：
- 开发模式: http://localhost:5173
- 生产模式: http://localhost:80

---

## 联系支持

如有问题，请提交 Issue：https://github.com/YUGUO-YU/legal-ai-assistant/issues
