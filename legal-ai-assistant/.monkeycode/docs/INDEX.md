# 法律AI助手系统 - 项目概览

## 项目简介

法律AI助手系统是一个基于人工智能的法律服务工具，为律师和法律从业者提供高效、智能的工作辅助。

## 技术栈

### 后端
- **框架**: Spring Boot 3.2
- **数据库**: MySQL 8.0
- **缓存**: Redis 7.x
- **搜索引擎**: Elasticsearch 8.x
- **向量数据库**: Milvus 2.4
- **AI**: MiniMax MoE-8x22B (OpenClaw)

### 前端
- **框架**: Vue 3.4+
- **UI组件**: Element Plus
- **HTTP客户端**: Axios
- **构建工具**: Vite 5.0
- **状态管理**: Pinia

## 功能模块

| 模块 | 功能 | 描述 | 状态 |
|------|------|------|------|
| MOD-01 | AI搜法 | 法规检索与溯源，ES+向量混合检索 | 已完成详细设计 |
| MOD-02 | AI类案 | 相似案例匹配，案件要素提取 | 已完成详细设计 |
| MOD-03 | AI文书起草 | 法律文书生成，20种模板 | 已完成详细设计 |
| MOD-04 | AI法律研究 | 结构化研究报告，SSE流式输出 | 已完成详细设计 |
| MOD-05 | 企业查询 | 企业工商信息查询，风险预警 | 已完成详细设计 |
| MOD-06 | 案例查询 | 司法判例检索，多维过滤 | 已完成详细设计 |
| MOD-07 | 法规查询 | 法规分类浏览，版本追溯 | 已完成详细设计 |
| MOD-08 | AI合同审查 | 合同风险分析，8维度评估 | 已完成详细设计 |
| MOD-09 | 案例法规库 | 知识库管理，文档上传 | 已完成详细设计 |
| MOD-10 | AI文件问答 | 文档智能问答，多轮对话 | 已完成详细设计 |

## 目录结构

```
legal-ai-assistant/
├── backend/                    # Spring Boot 后端
│   └── src/main/java/com/legalai/
│       ├── controller/        # REST控制器 (11个)
│       ├── service/           # 业务服务 (12个)
│       ├── model/             # 数据模型 (5个)
│       ├── dto/               # 数据传输对象 (20个)
│       ├── repository/        # MyBatis Mapper (5个)
│       ├── config/            # 配置类 (5个)
│       └── util/              # 工具类 (2个)
├── frontend/                  # Vue 3 前端
│   └── src/
│       ├── views/             # 页面组件 (14个)
│       ├── components/        # 公共组件 (5个)
│       ├── api/               # API调用封装
│       ├── store/             # Pinia状态管理
│       └── utils/             # 工具函数
├── .monkeycode/               # 项目规格文档
│   ├── docs/                  # 项目文档
│   │   ├── INDEX.md          # 项目概览
│   │   └── ARCHITECTURE.md   # 系统架构
│   └── specs/                 # 模块详细设计
│       ├── 01-ai-legal-search/   # AI搜法
│       ├── 02-ai-case-similar/   # AI类案
│       ├── 03-ai-document/      # AI文书起草
│       ├── 04-ai-legal-research/ # AI法律研究
│       ├── 05-company-query/    # 企业查询
│       ├── 06-case-search/      # 案例查询
│       ├── 07-law-search/       # 法规查询
│       ├── 08-contract-review/  # AI合同审查
│       ├── 09-knowledge-base/   # 案例法规库
│       └── 10-doc-qa/           # AI文件问答
└── docker-compose.yml         # Docker编排
```

## 基础设施

- MySQL 8.0 - 主数据库
- Redis 7.x - 缓存
- Elasticsearch 8.x - 搜索引擎
- Milvus 2.4 - 向量数据库

## 相关文档

- [架构文档](./docs/ARCHITECTURE.md)
- [各模块详细设计](./specs/)

## 模块详细设计索引

| 模块 | 设计文档 | 任务列表 |
|------|---------|---------|
| MOD-01 AI搜法 | [design.md](./specs/01-ai-legal-search/design.md) | [tasklist.md](./specs/01-ai-legal-search/tasklist.md) |
| MOD-02 AI类案 | [design.md](./specs/02-ai-case-similar/design.md) | [tasklist.md](./specs/02-ai-case-similar/tasklist.md) |
| MOD-03 AI文书起草 | [design.md](./specs/03-ai-document/design.md) | [tasklist.md](./specs/03-ai-document/tasklist.md) |
| MOD-04 AI法律研究 | [design.md](./specs/04-ai-legal-research/design.md) | [tasklist.md](./specs/04-ai-legal-research/tasklist.md) |
| MOD-05 企业查询 | [design.md](./specs/05-company-query/design.md) | [tasklist.md](./specs/05-company-query/tasklist.md) |
| MOD-06 案例查询 | [design.md](./specs/06-case-search/design.md) | [tasklist.md](./specs/06-case-search/tasklist.md) |
| MOD-07 法规查询 | [design.md](./specs/07-law-search/design.md) | [tasklist.md](./specs/07-law-search/tasklist.md) |
| MOD-08 AI合同审查 | [design.md](./specs/08-contract-review/design.md) | [tasklist.md](./specs/08-contract-review/tasklist.md) |
| MOD-09 案例法规库 | [design.md](./specs/09-knowledge-base/design.md) | [tasklist.md](./specs/09-knowledge-base/tasklist.md) |
| MOD-10 AI文件问答 | [design.md](./specs/10-doc-qa/design.md) | [tasklist.md](./specs/10-doc-qa/tasklist.md) |