# 案例法规库模块 - 实施计划

## 开发阶段

- [x] 1. 数据库表设计
  - [x] 1.1 创建knowledge_base表
  - [x] 1.2 创建kb_document表

- [x] 2. Milvus Collection配置
  - [x] 2.1 创建kb_embeddings Collection
  - [x] 2.2 配置HNSW索引

- [x] 3. 后端服务实现
  - [x] 3.1 创建KnowledgeBaseController
  - [x] 3.2 创建KnowledgeBaseService
  - [x] 3.3 实现知识库CRUD
  - [x] 3.4 实现文档上传

- [x] 4. 文档处理
  - [x] 4.1 文件格式校验
  - [x] 4.2 PDF文本提取
  - [x] 4.3 Word文本提取
  - [x] 4.4 TXT文本提取
  - [x] 4.5 Excel文本提取

- [x] 5. 语义分块
  - [x] 5.1 自适应分层切分
  - [x] 5.2 块大小控制（512 tokens）
  - [x] 5.3 块重叠处理（64 tokens）
  - [x] 5.4 页码标注

- [x] 6. 向量化索引
  - [x] 6.1 调用MiniMax Embedding
  - [x] 6.2 批量索引到Milvus
  - [x] 6.3 向量ID管理

- [x] 7. 团队共享
  - [x] 7.1 添加团队成员
  - [x] 7.2 移除团队成员
  - [x] 7.3 权限控制（查看/编辑）

- [x] 8. 前端交互开发
  - [x] 8.1 知识库列表
  - [x] 8.2 创建知识库
  - [x] 8.3 文档上传
  - [x] 8.4 上传进度展示
  - [x] 8.5 文档管理
  - [x] 8.6 团队共享设置

- [x] 9. 集成测试

- [x] 10. 检查点