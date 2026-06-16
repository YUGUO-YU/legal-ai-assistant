# AI搜法模块需求文档

## Introduction

AI搜法模块提供法规检索与溯源功能，支持ES+向量混合检索，帮助用户快速找到相关法律条文。

## Glossary

- **ES (Elasticsearch)**: 全文搜索引擎，用于关键词匹配检索
- **Milvus**: 开源向量数据库，用于语义相似度检索
- **混合检索**: 融合关键词检索和向量语义检索的结果
- **RRF (Reciprocal Rank Fusion)**: 倒数排序融合算法，用于多检索结果融合
- **溯源**: 标注法律结论的依据来源

## Requirements

### Requirement 1: 自然语言检索

**User Story:** 作为律师，我想要用自然语言描述法律问题，以便快速找到相关法规条文

#### Acceptance Criteria

1. WHEN 用户输入法律问题，THEN 系统 SHALL 支持中英文混合检索
2. WHEN 用户输入包含法律术语，THEN 系统 SHALL 支持同义词扩展检索
3. WHEN 检索结果返回，THEN 系统 SHALL 在100ms内完成基础检索（mock模式）

### Requirement 2: ES全文检索

**User Story:** 作为律师，我希望通过ES进行关键词精确匹配，找到包含特定词汇的法规

#### Acceptance Criteria

1. WHEN 用户输入检索词，THEN 系统 SHALL 在Elasticsearch中执行multi_match查询
2. WHEN 查询配置启用，THEN 系统 SHALL 使用BM25算法进行相关性排序
3. WHEN 搜索字段指定，THEN 系统 SHALL 支持lawTitle、articleNo、content、title字段搜索
4. WHEN 分页参数传入，THEN 系统 SHALL 返回分页结果，包含total、page、pageSize

### Requirement 3: Milvus向量检索

**User Story:** 作为律师，我希望通过语义理解找到相关法规，即使关键词不完全匹配

#### Acceptance Criteria

1. WHEN 用户输入检索词，THEN 系统 SHALL 调用嵌入模型生成向量表示
2. WHEN 向量生成完成，THEN 系统 SHALL 在Milvus中执行ANN向量检索
3. WHEN 向量检索配置启用，THEN 系统 SHALL 返回top-k相似结果
4. WHEN 向量维度为768，THEN 系统 SHALL 使用MiniMax嵌入服务

### Requirement 4: 混合检索融合

**User Story:** 作为律师，我希望混合检索结果融合，兼顾精确匹配和语义理解

#### Acceptance Criteria

1. WHEN ES检索和向量检索同时返回结果，THEN 系统 SHALL 使用RRF算法融合结果
2. WHEN 融合计算，THEN 系统 SHALL 使用公式 `score = Σ(1/(k+rank))` 计算综合得分
3. WHEN 融合权重配置，THEN 系统 SHALL 支持ES权重和向量权重可配置
4. WHEN 融合结果排序，THEN 系统 SHALL 按综合得分降序排列

### Requirement 5: 法规条文详情

**User Story:** 作为律师，我想要查看特定法规条文的完整内容

#### Acceptance Criteria

1. WHEN 用户点击法规条目，THEN 系统 SHALL 返回完整的法规内容
2. WHEN 详情查询，THEN 系统 SHALL 支持通过articleId获取单条记录
3. WHEN 返回字段完整，THEN 系统 SHALL 包含lawTitle、articleNo、title、content、sourceUrl、categoryL1、categoryL2

### Requirement 6: 相关案例推荐

**User Story:** 作为律师，我希望查看与检索法规相关的司法案例

#### Acceptance Criteria

1. WHEN 用户请求包含relatedCases，THEN 系统 SHALL 返回相关案例列表
2. WHEN 案例关联依据，THEN 系统 SHALL 基于法规主题匹配相关案例
3. WHEN 案例信息完整，THEN 系统 SHALL 返回caseUuid、caseNo、title、court、summary、sourceUrl

### Requirement 7: 追问建议生成

**User Story:** 作为律师，我希望获得追问建议，帮助我进一步深入研究

#### Acceptance Criteria

1. WHEN 检索结果返回，THEN 系统 SHALL 生成3-5个追问建议
2. WHEN 建议生成规则，THEN 系统 SHALL 基于用户查询主题生成相关问题
3. WHEN 建议分类，THEN 系统 SHALL 按法律领域（合同、劳动、侵权等）组织问题

### Requirement 8: 检索反馈

**User Story:** 作为律师，我希望对检索结果进行反馈，帮助系统学习改进

#### Acceptance Criteria

1. WHEN 用户对结果评分，THEN 系统 SHALL 支持3级评分（相关度）
2. WHEN 反馈提交，THEN 系统 SHALL 记录userId、articleId、rating、query、timestamp
3. WHEN 反馈数据积累，THEN 系统 SHALL 用于优化检索排序模型

### Requirement 9: 溯源标注

**User Story:** 作为律师，我需要知道法律结论的依据来源

#### Acceptance Criteria

1. WHEN AI生成法律结论，THEN 系统 SHALL 标注法规来源，格式为：[法规名称] 第X条
2. WHEN 来源信息完整，THEN 系统 SHALL 提供sourceUrl链接
3. WHEN 禁止胡编，THEN 系统 SHALL 只陈述检索结果中明确存在的内容

### Requirement 10: Mock模式支持

**User Story:** 作为开发/演示人员，我需要在没有外部依赖时测试系统

#### Acceptance Criteria

1. WHEN mock.enabled=true，THEN 系统 SHALL 使用内置Mock数据返回结果
2. WHEN Mock数据，THEN 系统 SHALL 内置8条代表性法规，覆盖民法、劳动合同法、司法解释
3. WHEN Mock模式切换，THEN 系统 SHALL 支持运行时动态切换