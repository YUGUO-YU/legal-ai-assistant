# 案例查询模块需求文档

## Introduction

案例查询模块提供司法判例检索功能，支持多维过滤、关键词搜索、分页浏览。

## Requirements

### Requirement 1: 多维过滤

**User Story:** 作为律师，我需要按多种条件过滤案例

#### Acceptance Criteria

1. WHEN 案件类型，THEN 系统 SHALL 支持民事、刑事、行政案件类型过滤
2. WHEN 法院层级，THEN 系统 SHALL 支持基层、中院、高院、最高院过滤
3. WHEN 审理程序，THEN 系统 SHALL 支持一审、二审、再审过滤
4. WHEN 判决结果，THEN 系统 SHALL 支持胜诉、败诉、部分胜诉过滤

### Requirement 2: 关键词搜索

**User Story:** 作为律师，我需要通过关键词搜索案例

#### Acceptance Criteria

1. WHEN 搜索字段，THEN 系统 SHALL 支持案号、当事人、法院、案情描述搜索
2. WHEN 搜索算法，THEN 系统 SHALL 使用ES全文检索
3. WHEN 高亮展示，THEN 系统 SHALL 关键词高亮显示

### Requirement 3: 分页浏览

**User Story:** 作为律师，我需要浏览大量案例

#### Acceptance Criteria

1. WHEN 分页参数，THEN 系统 SHALL 支持page和pageSize参数
2. WHEN 分页展示，THEN 系统 SHALL 返回总数量和分页数据
3. WHEN 每页条数，THEN 系统 SHALL 默认10条，最大100条

### Requirement 4: 案例详情

**User Story:** 作为律师，我需要查看案例详情

#### Acceptance Criteria

1. WHEN 详情字段，THEN 系统 SHALL 包含：案号、当事人、法院、审理经过、判决结果
2. WHEN 文书内容，THEN 系统 SHALL 支持查看完整判决文书
3. WHEN 来源链接，THEN 系统 SHALL 提供裁判文书网原文链接