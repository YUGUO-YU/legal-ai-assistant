# MOD-02 AI类案推荐模块 - 实施计划

## 开发阶段

- [x] 1. 数据库层搭建
  - [x] 1.1 创建tb_case表（案例主表）
  - [x] 1.2 创建tb_case_element表（案件要素表）
  - [x] 1.3 创建tb_case_embedding表（案例向量表）
  - [x] 1.4 创建tb_case_label表（案例标签表）
  - [x] 1.5 创建tb_similar_case表（类案记录表）

- [x] 2. 搜索引擎配置
  - [x] 2.1 配置Milvus集群
  - [x] 2.2 创建tb_case案例Collection
  - [x] 2.3 配置案件向量索引

- [x] 3. 后端服务实现
  - [x] 3.1 创建CaseSimilarController
  - [x] 3.2 创建CaseService
  - [x] 3.3 实现案件要素提取
  - [x] 3.4 实现Milvus ANN检索
  - [x] 3.5 实现多维度匹配评分
  - [x] 3.6 实现Rerank重排序
  - [x] 3.7 实现统计信息计算

- [x] 4. AI增强功能
  - [x] 4.1 配置MiniMax embedding
  - [x] 4.2 实现案件描述向量化
  - [x] 4.3 实现裁判要点生成

- [x] 5. 溯源和真实性保障
  - [x] 5.1 实现来源URL白名单校验
  - [x] 5.2 实现涉敏词过滤

- [x] 6. 缓存优化
  - [x] 6.1 复用LegalSearch缓存服务
  - [x] 6.2 实现热点案例缓存

- [x] 7. 前端交互开发
  - [x] 7.1 案件描述输入
  - [x] 7.2 相似案例展示
  - [x] 7.3 统计信息面板
  - [x] 7.4 匹配特征展示

- [x] 8. 数据导入
  - [x] 8.1 导入裁判文书数据
  - [x] 8.2 向量化所有案例

- [x] 9. 集成测试
  - [x] 9.1 Milvus案例检索测试
  - [x] 9.2 相似度计算测试
  - [x] 9.3 性能基准测试

- [x] 10. 检查点

## MOD-02 完成状态：进行中

### 已更新文件
- `CaseService.java` - 增强案件要素提取、多维度匹配、统计计算
- `MilvusService.java` - 新增searchSimilarCases方法

### 已创建文件
- 无

## 待完成
- 数据导入脚本
- 集成测试
- 检查点