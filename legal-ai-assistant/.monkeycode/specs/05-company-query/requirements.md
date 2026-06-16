# 企业查询模块需求文档

## Introduction

企业查询模块提供企业工商信息查询功能，支持企业工商信息查询、股东结构分析、司法风险预警。

## Requirements

### Requirement 1: 企业信息查询

**User Story:** 作为律师，我需要查询企业的工商信息

#### Acceptance Criteria

1. WHEN 用户输入企业名称或统一社会信用代码，THEN 系统 SHALL 返回企业基本信息
2. WHEN 基本信息包含，THEN 系统 SHALL 包含：企业名称、统一社会信用代码、法定代表人、注册资本、成立日期、经营状态
3. WHEN 经营状态，THEN 系统 SHALL 支持在业、存续、吊销、注销等状态

### Requirement 2: 股东结构分析

**User Story:** 作为律师，我需要了解企业的股东结构

#### Acceptance Criteria

1. WHEN 查询完成，THEN 系统 SHALL 返回股东列表和持股比例
2. WHEN 股东穿透，THEN 系统 SHALL 支持穿透查看实际控制人
3. WHEN 股东信息，THEN 系统 SHALL 包含：股东名称、持股比例、出资额、出资方式

### Requirement 3: 司法风险预警

**User Story:** 作为律师，我需要了解企业的法律风险

#### Acceptance Criteria

1. WHEN 风险查询，THEN 系统 SHALL 返回企业涉诉信息
2. WHEN 风险类型，THEN 系统 SHALL 包含：被执行人、失信被执行人、裁判文书、行政处罚
3. WHEN 风险等级，THEN 系统 SHALL 按无风险/低风险/中风险/高风险四级展示

### Requirement 4: 多数据源交叉验证

**User Story:** 作为系统，我需要确保查询结果的准确性

#### Acceptance Criteria

1. WHEN 数据来源，THEN 系统 SHALL 整合企查查、天眼查等第三方API
2. WHEN 交叉验证，THEN 系统 SHALL 对关键字段进行多源比对
3. WHEN 数据不一致，THEN 系统 SHALL 标注数据来源和置信度

### Requirement 5: Mock模式支持

**User Story:** 作为开发/演示人员，我需要在没有外部API时测试系统

#### Acceptance Criteria

1. WHEN mock.enabled=true，THEN 系统 SHALL 使用Mock数据返回结果
2. WHEN Mock数据，THEN 系统 SHALL 内置代表性企业示例数据
3. WHEN 模式切换，THEN 系统 SHALL 支持运行时动态切换