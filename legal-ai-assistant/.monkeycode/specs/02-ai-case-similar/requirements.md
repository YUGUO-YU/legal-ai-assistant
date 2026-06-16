# AI类案模块需求文档

## Introduction

AI类案模块提供相似案例匹配功能，通过案件要素提取和向量检索，帮助律师快速找到类似判例。

## Glossary

- **类案**: 相似案例，指与当前案件在事实、诉求、争议焦点等方面相似的已决案例
- **案件要素**: 包括当事人信息、案件类型、事实描述、诉讼请求、法律关系等
- **相似度**: 衡量两个案件相似程度的指标，通常为0-1之间的分数
- **裁判要点**: 法院在判决中对案件关键法律问题的认定和说理
- **向量检索**: 将案件文本转换为向量，在向量空间中查找相似案件

## Requirements

### Requirement 1: 案件描述输入

**User Story:** 作为律师，我想要输入案件描述，以便系统找到相似案例

#### Acceptance Criteria

1. WHEN 用户输入案件描述，THEN 系统 SHALL 支持文本输入，最长支持5000字
2. WHEN 案件类型选择，THEN 系统 SHALL 支持按案件类型过滤（民事、刑事、行政）
3. WHEN 法院层级选择，THEN 系统 SHALL 支持按法院层级过滤（基层、中院、高院、最高院）

### Requirement 2: 案件要素提取

**User Story:** 作为律师，我希望系统自动提取案件要素，便于精准匹配

#### Acceptance Criteria

1. WHEN 用户提交案件描述，THEN 系统 SHALL 调用AI提取关键要素
2. WHEN 要素提取完成，THEN 系统 SHALL 提取：案件类型、争议焦点、标的金额、当事人角色、法律关系
3. WHEN 要素结构化，THEN 系统 SHALL 返回JSON格式的结构化要素
4. WHEN 提取失败，THEN 系统 SHALL 返回原始描述并标注提取失败原因

### Requirement 3: 相似案例匹配

**User Story:** 作为律师，我希望系统基于案件描述找到相似案例

#### Acceptance Criteria

1. WHEN 用户提交案件描述，THEN 系统 SHALL 执行向量检索匹配相似案例
2. WHEN 检索范围，THEN 系统 SHALL 支持自定义返回相似案例数量（topK）
3. WHEN 匹配结果，THEN 系统 SHALL 返回相似度分数（0-1之间）
4. WHEN 匹配维度，THEN 系统 SHALL 支持fact_similarity、claim_similarity、dispute_similarity多维度

### Requirement 4: 裁判要点分析

**User Story:** 作为律师，我希望查看相似案例的裁判要点，了解法院认定逻辑

#### Acceptance Criteria

1. WHEN 案例详情查询，THEN 系统 SHALL 返回裁判要点分析
2. WHEN 要点内容，THEN 系统 SHALL 包含：争议焦点、法院认定、判决理由、法律适用
3. WHEN 要点格式，THEN 系统 SHALL 使用结构化文本，便于阅读

### Requirement 5: 类案判决结果统计

**User Story:** 作为律师，我希望获得类案判决结果的统计分析

#### Acceptance Criteria

1. WHEN 类案检索完成，THEN 系统 SHALL 生成统计报告
2. WHEN 统计数据，THEN 系统 SHALL 包含：总案例数、胜诉率、平均赔偿金额、判决类型分布
3. WHEN 统计维度，THEN 系统 SHALL 按法院层级、判决结果、赔偿金额区间统计
4. WHEN 图表生成，THEN 系统 SHALL 支持返回ECharts格式的图表配置数据

### Requirement 6: 法律依据推荐

**User Story:** 作为律师，我希望获得类案涉及的法律依据

#### Acceptance Criteria

1. WHEN 案例匹配完成，THEN 系统 SHALL 推荐相关法律依据
2. WHEN 推荐依据，THEN 系统 SHALL 包含法规名称和具体条文
3. WHEN 依据排序，THEN 系统 SHALL 按引用频次降序排列

### Requirement 7: 案例详情查看

**User Story:** 作为律师，我希望查看相似案例的完整文书内容

#### Acceptance Criteria

1. WHEN 用户点击案例，THEN 系统 SHALL 支持查看案例详情
2. WHEN 详情内容，THEN 系统 SHALL 包含：案号、当事人、法院、审理经过、判决结果
3. WHEN 文书来源，THEN 系统 SHALL 提供原始链接（裁判文书网）

### Requirement 8: 案例对比

**User Story:** 作为律师，我希望对比多个相似案例的关键差异

#### Acceptance Criteria

1. WHEN 用户选择多个案例，THEN 系统 SHALL 支持案例对比
2. WHEN 对比维度，THEN 系统 SHALL 支持：事实相似度、判决结果、赔偿金额、审理时长
3. WHEN 对比展示，THEN 系统 SHALL 使用表格形式展示对比结果

### Requirement 9: Mock模式支持

**User Story:** 作为开发/演示人员，我需要在没有外部依赖时测试系统

#### Acceptance Criteria

1. WHEN mock.enabled=true，THEN 系统 SHALL 使用内置Mock数据返回结果
2. WHEN Mock数据，THEN 系统 SHALL 内置5条代表性案例，覆盖不同案件类型
3. WHEN Mock模式切换，THEN 系统 SHALL 支持运行时动态切换