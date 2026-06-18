# MOD-01 AI搜法模块 - 实施计划

## 开发阶段

- [x] 1. 数据库层搭建
  - [x] 1.1 创建law_document表（法规主表）
  - [x] 1.2 创建law_article表（法规条款表）
  - [x] 1.3 创建legal_case表（判例表）
  - [x] 1.4 创建search_log表（搜索日志）
  - [x] 1.5 创建search_feedback表（搜索反馈）
  - [x] 1.6 创建law_relation表（法规关联表）

- [x] 2. 搜索引擎配置
  - [x] 2.1 配置Elasticsearch集群
  - [x] 2.2 创建legal_law_articles索引
  - [x] 2.3 配置ik_smart分词器
  - [x] 2.4 配置Milvus集群
  - [x] 2.5 创建legal_law_articles Collection（HNSW索引）

- [x] 3. 后端服务实现
  - [x] 3.1 创建LegalSearchController
  - [x] 3.2 创建LegalSearchService
  - [x] 3.3 创建ElasticsearchService
  - [x] 3.4 创建MilvusService
  - [x] 3.5 实现Query理解和意图识别
  - [x] 3.6 实现同义词扩展
  - [x] 3.7 实现ES BM25检索
  - [x] 3.8 实现Milvus ANN检索
  - [x] 3.9 实现RRF融合算法
  - [x] 3.10 实现Rerank重排序

- [x] 4. AI增强功能
  - [x] 4.1 配置 MiniMax-M3 API
  - [x] 4.2 实现System Prompt注入
  - [x] 4.3 实现答案生成
  - [x] 4.4 实现引用溯源标注
  - [x] 4.5 实现追问建议生成
  - [x] 4.6 实现AI幻觉检测

- [x] 5. 溯源和真实性保障
  - [x] 5.1 实现来源URL白名单校验
  - [x] 5.2 实现citation格式标准化
  - [x] 5.3 实现超时降级处理
  - [x] 5.4 实现涉敏词过滤

- [x] 6. 缓存优化
  - [x] 6.1 配置Redis缓存
  - [x] 6.2 实现热点法规缓存
  - [x] 6.3 实现搜索结果缓存

- [x] 7. 前端交互开发
  - [x] 7.1 法规搜索页面
  - [x] 7.2 条款详情页面
  - [x] 7.3 搜索结果高亮展示
  - [x] 7.4 相关判例推荐
  - [x] 7.5 追问建议点击交互
  - [x] 7.6 搜索反馈组件

- [x] 8. 数据导入
  - [x] 8.1 导入民法典（1270条）
  - [x] 8.2 导入劳动合同法及相关法规
  - [x] 8.3 导入建设工程司法解释
  - [x] 8.4 向量化所有法规条文

- [x] 9. 集成测试
  - [x] 9.1 ES检索功能测试
  - [x] 9.2 Milvus向量检索测试
  - [x] 9.3 混合检索融合测试
  - [x] 9.4 AI生成质量测试
  - [x] 9.5 性能基准测试（<500ms）

- [x] 10. 检查点 - 确保所有核心功能可运行
  - [x] 确保ES和Milvus连接正常
  - [x] 确保混合检索融合算法正确
  - [x] 确保溯源标注功能正常

## MOD-01 完成状态：已完成

### 已创建文件
- `MilvusConfig.java` - Milvus客户端配置
- `MilvusService.java` - 向量检索服务
- `ElasticsearchConfig.java` - ES客户端配置
- `ElasticsearchService.java` - 全文检索服务
- `SourceVerificationService.java` - 来源验证服务
- `CacheService.java` - Redis缓存服务
- `DataImportService.java` - 数据导入服务
- `DataImportController.java` - 数据导入控制器
- `Citation.java` - 引用溯源DTO
- `SearchFeedbackRequest.java` - 搜索反馈请求DTO
- `LegalSearchController.java` - 更新：新增suggested-queries和feedback接口

### 已更新文件
- `LegalSearchService.java` - 集成缓存、超时降级、敏感词过滤
- `LegalSearchController.java` - 新增getArticleDetail和submitFeedback方法
- `LegalSearch.vue` - 新增溯源标签、追问建议、反馈功能
- `api/index.js` - 新增getSuggestedQueries接口