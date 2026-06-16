# 法律AI助手系统 - 系统架构

## 整体架构

```mermaid
graph TB
    subgraph Frontend["前端 Vue 3"]
        UI["Element Plus UI"]
        Store["Pinia 状态管理"]
        Router["Vue Router"]
        Api["Axios API层"]
    end

    subgraph Backend["后端 Spring Boot"]
        Controller["REST Controllers"]
        Service["Business Services"]
        Repository["Data Repositories"]
        Cache["Redis Cache"]
        AI["OpenClaw/MiniMax AI"]
    end

    subgraph DataLayer["数据层"]
        MySQL["MySQL 8.0"]
        ES["Elasticsearch 8.x"]
        Milvus["Milvus 2.4"]
    end

    subgraph External["外部服务"]
        MiniMax["MiniMax AI API"]
        ThirdParty["企查查/天眼查/信用中国"]
    end

    UI --> Api
    Api --> Controller
    Controller --> Service
    Service --> Repository
    Service --> Cache
    Repository --> MySQL
    Service --> ES
    Service --> Milvus
    Service --> AI
    Service --> ThirdParty
```

## 后端架构

```mermaid
graph LR
    subgraph Controllers["控制器层"]
        Auth["AuthController"]
        LegalSearch["LegalSearchController"]
        CaseSearch["CaseSearchController"]
        CaseSimilar["CaseSimilarController"]
        Contract["ContractReviewController"]
        DocQa["DocQaController"]
        Document["DocumentController"]
        Company["CompanyQueryController"]
        LawSearch["LawSearchController"]
        LegalResearch["LegalResearchController"]
        KnowledgeBase["KnowledgeBaseController"]
    end

    subgraph Services["服务层"]
        AuthService
        LegalSearchService
        CaseService
        CaseSimilarService
        ContractService
        DocQaService
        DocumentService
        CompanyService
        LawSearchService
        LegalResearchService
        KnowledgeBaseService
        AIService
    end

    subgraph Repositories["仓储层"]
        CaseInfoMapper
        LegalCaseMapper
        LawDocumentMapper
        LawArticleMapper
        SearchLogMapper
    end

    Controllers --> Services
    Services --> Repositories
    Services --> AI
```

## 前端架构

```mermaid
graph TB
    subgraph Views["页面层"]
        Login["登录"]
        Dashboard["工作台"]
        LegalSearch["AI搜法"]
        CaseSimilar["AI类案"]
        CaseSearch["案例查询"]
        LawSearch["法规查询"]
        Document["AI文书起草"]
        LegalResearch["AI法律研究"]
        CompanyQuery["企业查询"]
        ContractReview["AI合同审查"]
        KnowledgeBase["案例法规库"]
        DocQa["AI文件问答"]
        Profile["个人设置"]
        NotFound["404页面"]
    end

    subgraph Components["组件层"]
        Loading
        Toast
        EmptyState
        ErrorBoundary
    end

    subgraph Store["状态层"]
        UserStore
        SearchStore
        KBStore
    end

    subgraph Api["API层"]
        legalSearch
        caseSimilar
        caseSearch
        docQa
        contract
        document
        company
        lawSearch
        legalResearch
        knowledgeBase
        auth
    end

    Views --> Store
    Views --> Components
    Views --> Api
```

## API 网关模式

所有前端请求通过 `/api/v1` 前缀路由到后端：

```
/api/v1/auth/*              # 认证相关
/api/v1/legal-search/*      # AI搜法
/api/v1/case-similar/*      # AI类案
/api/v1/case-search/*      # 案例查询
/api/v1/law-search/*        # 法规查询
/api/v1/document/*         # AI文书起草
/api/v1/legal-research/*   # AI法律研究
/api/v1/company/*          # 企业查询
/api/v1/contract/*         # AI合同审查
/api/v1/doc-qa/*           # AI文件问答
/api/v1/knowledge-base/*   # 知识库
```

## 核心数据流

### AI搜法流程
```
用户输入查询 → Query理解 → 意图识别 →
ES BM25检索 + Milvus ANN检索 → RRF融合 →
Rerank重排序 → OpenClaw生成回答 → 引用溯源 → 返回结果
```

### AI类案流程
```
用户输入案件描述 → 案件要素提取 → 向量化 →
Milvus ANN检索 → 多维度相似度计算 →
Rerank重排序 → 裁判要点生成 → 返回类案列表
```

### AI合同审查流程
```
上传合同 → 文本提取 → 8维度AI审查 →
风险分级 → 改进建议生成 → 审查报告 → 返回结果
```

### 文档问答流程
```
用户提问 → 混合检索 → RRF融合 →
上下文构建 → OpenClaw生成答案 → 引用溯源 → 返回结果
```

## 安全架构

- JWT Token 认证
- 请求限流（Redis）
- CORS 配置
- 参数校验
- AI幻觉检测

## 扩展性设计

- 微服务架构预留
- 配置中心支持
- 插件化AI provider
- 多数据源支持