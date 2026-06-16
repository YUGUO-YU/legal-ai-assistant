# 法规查询模块需求文档

## Introduction

法规查询模块提供法规分类浏览和版本追溯功能。

## Requirements

### Requirement 1: 分类树导航

**User Story:** 作为律师，我需要按分类浏览法规

#### Acceptance Criteria

1. WHEN 法规分类，THEN 系统 SHALL 包含：法律、行政法规、司法解释、部门规章
2. WHEN 二级分类，THEN 系统 SHALL 按部门法分类（民法、刑法、行政法等）
3. WHEN 分类展示，THEN 系统 SHALL 使用树形结构展示

### Requirement 2: 法规状态管理

**User Story:** 作为系统，我需要标注法规的有效性

#### Acceptance Criteria

1. WHEN 状态标注，THEN 系统 SHALL 标注：有效、已被修改、已废止
2. WHEN 版本信息，THEN 系统 SHALL 标注法规的发布时间和修订时间
3. WHEN 变更提示，THEN 系统 SHALL 提示已修改法规的历次修订

### Requirement 3: 版本追溯

**User Story:** 作为律师，我需要查看法规的历史版本

#### Acceptance Criteria

1. WHEN 历史版本，THEN 系统 SHALL 支持查看法规的历次修订版本
2. WHEN 版本对比，THEN 系统 SHALL 支持对比不同版本的差异
3. WHEN 引用旧版，THEN 系统 SHALL 标注案件引用法规的适用版本

### Requirement 4: 关联推荐

**User Story:** 作为律师，我需要获取相关法规推荐

#### Acceptance Criteria

1. WHEN 关联依据，THEN 系统 SHALL 基于当前法规推荐相关法规
2. WHEN 关联类型，THEN 系统 SHALL 包含：同主题法规、上位法、下位法、配套规定