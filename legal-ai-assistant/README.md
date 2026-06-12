# 法律AI助手系统 (Legal AI Assistant)

律师法律助手系统是一个基于人工智能的法律服务工具，包含十大核心功能模块，为律师和法律从业者提供高效、智能的工作辅助。

## 功能模块

| 模块 | 功能 | 描述 |
|------|------|------|
| MOD-01 | AI搜法 | 法规检索与溯源，ES+向量混合检索 |
| MOD-02 | AI类案 | 相似案例匹配，案件要素提取 |
| MOD-03 | AI文书起草 | 法律文书生成，20种模板 |
| MOD-04 | AI法律研究 | 结构化研究报告，SSE流式输出 |
| MOD-05 | 企业查询 | 企业工商信息查询，风险预警 |
| MOD-06 | 案例查询 | 司法判例检索，多维过滤 |
| MOD-07 | 法规查询 | 法规分类浏览，版本追溯 |
| MOD-08 | AI合同审查 | 合同风险分析，8维度评估 |
| MOD-09 | 案例法规库 | 知识库管理，文档上传 |
| MOD-10 | AI文件问答 | 文档智能问答，多轮对话 |

## 技术栈

### 后端
- **框架**: Spring Boot 3.2
- **数据库**: MySQL 8.0
- **缓存**: Redis 7.x
- **搜索引擎**: Elasticsearch 8.x
- **向量数据库**: Milvus 2.4
- **AI**: OpenClaw (Minimax MoE 8x22B)

### 前端
- **框架**: Vue 3.4+
- **UI组件**: Element Plus
- **HTTP客户端**: Axios
- **构建工具**: Vite 5.0
- **状态管理**: Pinia

## 项目结构

```
legal-ai-assistant/
├── backend/
│   ├── src/main/java/com/legalai/
│   │   ├── config/           # 配置类 (5个)
│   │   │   ├── RedisConfig.java
│   │   │   ├── WebMvcConfig.java
│   │   │   ├── MybatisPlusConfig.java
│   │   │   └── GlobalExceptionHandler.java
│   │   ├── controller/       # REST控制器 (11个)
│   │   │   ├── AuthController.java
│   │   │   ├── LegalSearchController.java
│   │   │   ├── CaseSearchController.java
│   │   │   ├── CaseSimilarController.java
│   │   │   ├── DocumentController.java
│   │   │   ├── CompanyQueryController.java
│   │   │   ├── ContractReviewController.java
│   │   │   ├── DocQaController.java
│   │   │   └── LawSearchController.java
│   │   ├── service/ # 业务服务 (10个)
│   │   │   ├── AuthService.java
│   │   │   ├── LegalSearchService.java
│   │   │   ├── CaseSearchService.java
│   │   │   ├── CaseService.java
│   │   │   ├── DocumentService.java
│   │   │   ├── CompanyService.java
│   │   │   ├── ContractService.java
│   │   │   ├── DocQaService.java
│   │   │   └── LawSearchService.java
│   │   ├── repository/       # MyBatis Mapper (5个)
│   │   ├── model/           # 数据模型 (5个)
│   │   ├── dto/             # 数据传输对象 (18个)
│   │   ├── util/            # 工具类 (2个)
│   │   │   ├── ValidationUtils.java
│   │   │   └── IdGenerator.java
│   │   ├── config/          # 配置类 (5个)
│   │   │   ├── RedisConfig.java
│   │   │   ├── WebMvcConfig.java
│   │   │   ├── MybatisPlusConfig.java
│   │   │   └── GlobalExceptionHandler.java
│   │   └── LegalAiApplication.java
│   └── src/main/resources/
│       ├── application.yml
│       └── schema.sql
│
├── frontend/
│   ├── src/
│   │   ├── views/            # 页面组件 (14个)
│   │   │   ├── Login.vue
│   │   │   ├── Dashboard.vue
│   │   │   ├── LegalSearch.vue
│   │   │   ├── CaseSimilar.vue
│   │   │   ├── CaseSearch.vue
│   │   │   ├── LawSearch.vue
│   │   │   ├── Document.vue
│   │   │   ├── LegalResearch.vue
│   │   │   ├── CompanyQuery.vue
│   │   │   ├── ContractReview.vue
│   │   │   ├── KnowledgeBase.vue
│   │   │   ├── DocQa.vue
│   │   │   ├── Profile.vue
│   │   │   └── NotFound.vue
│   │   ├── components/       # 通用组件 (5个)
│   │   ├── store/            # Pinia状态管理 (3个)
│   │   ├── router/           # 路由配置
│   │   ├── api/              # API调用封装
│   │   ├── utils/            # 工具函数
│   │   └── assets/           # 静态资源
│   └── package.json
│
└── README.md
```

## 快速开始

### 环境要求
- Node.js 18+
- JDK 17+
- Maven 3.8+

### Windows 环境准备

#### 1. 安装 Node.js 18+
访问 https://nodejs.org/ 下载 Windows 安装包（.msi），建议选择 LTS 版本。

安装完成后验证：
```powershell
node -v
npm -v
```

#### 2. 安装 JDK 17+
访问 https://adoptium.net/ 下载 JDK 17 Windows 安装包。

安装完成后配置环境变量：
```powershell
# 新增系统环境变量
JAVA_HOME = C:\Program Files\Eclipse Adoptium\jdk-17.0.x.x

# 在 Path 中添加
%JAVA_HOME%\bin
```

验证：
```powershell
java -version
javac -version
```

#### 3. 安装 Maven 3.8+
访问 https://maven.apache.org/download.cgi 下载 zip 包。

配置环境变量：
```powershell
# 新增系统环境变量
MAVEN_HOME = C:\apache-maven-3.8.x

# 在 Path 中添加
%MAVEN_HOME%\bin
```

验证：
```powershell
mvn -version
```

#### 4. 安装 Git
访问 https://git-scm.com/download/win 下载安装包。

验证：
```powershell
git --version
```

---

### 方式一：Mock模式（无需外部依赖，推荐演示使用）

Mock模式使用模拟数据，无需配置MySQL、Redis等外部服务。

#### 1. 克隆代码
```powershell
git clone https://github.com/YUGUO-YU/legal-ai-assistant.git
cd legal-ai-assistant
```

#### 2. 启动后端
```powershell
cd backend
mvn spring-boot:run
```
后端运行在 http://localhost:3001

#### 3. 启动前端
```powershell
cd frontend
npm install
npm run dev
```
前端运行在 http://localhost:5173

#### 4. 访问应用
打开浏览器访问 http://localhost:5173

登录：任意用户名，密码长度>=6位即可

---

### 方式二：生产环境部署

#### 环境要求
- Node.js 18+
- JDK 17+
- MySQL 8.0+
- Redis 7.x

#### 1. 克隆代码
```powershell
git clone https://github.com/YUGUO-YU/legal-ai-assistant.git
cd legal-ai-assistant
```

#### 2. 安装并配置 MySQL 8.0
下载 MySQL https://dev.mysql.com/downloads/mysql/

初始化数据库：
```powershell
mysql -u root -p < backend\src\main\resources\schema.sql
```

#### 3. 修改配置
编辑 `backend\src\main\resources\application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/legal_ai
    username: your_username
    password: your_password
  data:
    redis:
      host: localhost
      port: 6379

mock:
  enabled: false  # 关闭Mock模式
```

#### 4. 打包后端
```powershell
cd backend
mvn clean package -DskipTests
```

#### 5. 启动后端
```powershell
java -jar target\legal-ai-assistant-1.0.0.jar
```

#### 6. 构建前端
```powershell
cd frontend
npm install
npm run build
```

#### 7. Nginx配置（Windows版）
下载 Nginx for Windows http://nginx.org/en/download.html

配置 `nginx.conf`:
```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 前端静态文件
    location / {
        root C:\path\to\legal-ai-assistant\frontend\dist;
        try_files $uri $uri/ /index.html;
    }

    # API代理
    location /api {
        proxy_pass http://localhost:3001;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

启动Nginx：
```powershell
nginx.exe
```

---

### Docker部署（推荐）

#### Windows上使用Docker Desktop
1. 下载并安装 Docker Desktop https://www.docker.com/products/docker-desktop/
2. 启动 Docker Desktop
3. 等待托盘图标显示 Docker 运行中

#### 启动服务
```powershell
# 构建并启动
docker-compose up -d

# 查看日志
docker-compose logs -f

# 停止
docker-compose down
```

## 功能详情

### 1. AI搜法
- 支持自然语言查询
- 法规条文检索与溯源标注
- 相关案例推荐
- 追问建议生成

### 2. AI类案
- 案件描述输入
- 相似案例智能匹配
- 裁判要点分析
- 类案判决结果统计

### 3. AI文书起草
- 15种文书模板
- 智能填充案件信息
- 风险提示自动生成
- 免责声明

### 4. AI法律研究
- 多维度研究问题分析
- 结构化报告生成
- 流式进度展示
- 法规/判例/学术观点聚合

### 5. 企业查询
- 工商信息查询
- 股东结构分析
- 司法风险预警
- 多数据源交叉验证

### 6. 案例查询
- 多维过滤（案件类型/法院/程序/结果）
- 关键词搜索
- 分页浏览
- 案例详情

### 7. 法规查询
- 分类树导航
- 法规状态管理
- 版本追溯
- 关联推荐

### 8. AI合同审查
- 8维度风险评估
- 三级风险分级
- 逐项改进建议
- 审查报告导出

### 9. 案例法规库
- 文档上传管理
- 语义分块
- 团队共享
- 检索问答

### 10. AI文件问答
- 混合检索
- 多轮上下文理解
- 引用溯源
- 会话历史

## API 接口

| 方法 | 路径 | 功能 |
|------|------|------|
| POST | /api/v1/auth/login | 用户登录 |
| POST | /api/v1/auth/logout | 用户登出 |
| GET | /api/v1/auth/user-info | 获取用户信息 |
| POST | /api/v1/legal-search/search | 法规检索 |
| GET | /api/v1/legal-search/articles/{id} | 条款详情 |
| POST | /api/v1/case-similar/search | 类案检索 |
| POST | /api/v1/case-search/search | 案例查询 |
| POST | /api/v1/law-search/search | 法规查询 |
| GET | /api/v1/law-search/categories | 获取法规分类 |
| POST | /api/v1/document/draft | 文书起草 |
| GET | /api/v1/document/templates | 获取文书模板 |
| POST | /api/v1/company/query | 企业查询 |
| POST | /api/v1/contract/review | 合同审查 |
| GET | /api/v1/contract/dimensions | 获取审查维度 |
| POST | /api/v1/doc-qa/ask | 文档问答 |
| GET | /api/v1/health | 健康检查 |

## 配置说明

### application.yml

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/legal_ai
    username: root
    password: your_password
  data:
    redis:
      host: localhost
      port: 6379
  elasticsearch:
    uris: http://localhost:9200

ai:
  openclaw:
    api-url: https://api.openclaw.com
    api-key: your_api_key
    model: MoE-8x22B

mock:
  enabled: true  # 设为 false 使用真实API
```

## 前端结构

```
frontend/
├── src/
│   ├── views/            # 页面组件 (14个)
│   │   ├── Login.vue
│   │   ├── Dashboard.vue      # 工作台
│   │   ├── LegalSearch.vue    # AI搜法
│   │   ├── CaseSimilar.vue    # AI类案
│   │   ├── CaseSearch.vue     # 案例查询
│   │   ├── LawSearch.vue      # 法规查询
│   │   ├── Document.vue       # AI文书起草
│   │   ├── LegalResearch.vue  # AI法律研究
│   │   ├── CompanyQuery.vue   # 企业查询
│   │   ├── ContractReview.vue # AI合同审查
│   │   ├── KnowledgeBase.vue  # 案例法规库
│   │   ├── DocQa.vue          # AI文件问答
│   │   ├── Profile.vue        # 个人设置
│   │   └── NotFound.vue       # 404页面
│   ├── components/       # 通用组件 (5个)
│   │   ├── Loading.vue        # 加载状态
│   │   ├── EmptyState.vue     # 空状态
│   │   ├── ErrorBoundary.vue  # 错误边界
│   │   ├── Toast.vue          # Toast通知
│   │   └── ToastManager.vue   # Toast管理器
│   ├── api/              # API调用封装
│   ├── store/            # Pinia状态管理
│   ├── router/           # 路由配置
│   ├── utils/            # 工具函数
│   └── assets/           # 静态资源
```

## 前端工具函数

```javascript
// 格式化
formatDate(date, 'YYYY-MM-DD')
formatMoney(12345.67, '¥')
formatNumber(1234567,2)

// 验证
validatePhone('13800138000')
validateEmail('test@example.com')
validateIdCard('110101199001011234')

// 剪贴板
copyToClipboard('text')

// 下载文件
downloadFile(content, 'filename.txt')
```

## 注意事项

1. **模拟数据模式**: 当前版本 `mock.enabled=true`，使用模拟数据演示功能
2. **外部API**: 企查查、天眼查等外部API需配置真实密钥
3. **基础设施**: 生产环境需部署 MySQL、Redis、Elasticsearch、Milvus
4. **登录演示**: 用户名任意，密码长度>=6位即可

## Windows 常见问题

### 1. Maven 构建失败
如果遇到 `mvn` 命令找不到，确保环境变量配置正确：
```powershell
# 检查 Maven 是否配置
echo $env:MAVEN_HOME

# 临时添加（当前命令行窗口有效）
$env:Path += ";C:\apache-maven-3.8.x\bin"
```

### 2. 端口被占用
如果提示端口 3001 或 5173 被占用：
```powershell
# 查看端口占用
netstat -ano | findstr :3001

# 结束进程（替换 PID 为实际进程ID）
taskkill /PID <PID> /F
```

### 3. npm install 失败
清理缓存后重试：
```powershell
npm cache clean --force
rm -rf node_modules
npm install
```

### 4. 后端启动失败
检查 JDK 版本：
```powershell
java -version  # 确保是 JDK 17+
```

### 5. 前端无法连接后端
如果前端运行在 http://localhost:5173，后端运行在 http://localhost:3001，确保后端配置了 CORS。

检查 `backend/src/main/resources/application.yml` 中的端口配置是否正确。登录

## 开发说明

### 添加新模块

1. 后端：在 `service/` 下创建业务逻辑，在 `controller/` 下创建 REST API
2. 前端：在 `views/` 下创建 Vue 组件，在 `router/` 下添加路由
3. 在 `api/index.js` 中添加对应的 API 调用方法

### 代码规范

- 命名：驼峰命名
- 注释：关键逻辑添加中文注释
- 提交：提交前检查改动

## License

MIT License