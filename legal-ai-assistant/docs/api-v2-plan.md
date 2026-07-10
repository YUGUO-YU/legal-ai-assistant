# API v2 规划文档

## 版本策略

当前 API 版本: `v1`
目标 API 版本: `v2`

### 版本共存策略
- v1 和 v2 在过渡期共存
- v1 继续维护但不再增加新功能
- v2 是 v1 的超集，包含所有 v1 功能

## v2 新特性

### 1. 统一的分页响应格式

```json
{
  "success": true,
  "data": [...],
  "pagination": {
    "page": 1,
    "pageSize": 20,
    "total": 100,
    "totalPages": 5,
    "hasNext": true,
    "hasPrev": false
  },
  "meta": {
    "requestId": "trace-xxx",
    "timestamp": 1699999999999
  }
}
```

### 2. 字段选择 (Sparse Fields)

```
GET /api/v2/laws?fields=id,title,effectiveDate
GET /api/v2/cases?fields=caseUuid,title,court,judgeDate
```

### 3. 批量操作

```
POST /api/v2/laws/batch
DELETE /api/v2/laws/batch
PATCH /api/v2/laws/batch
```

### 4. 过滤与排序

```
GET /api/v2/laws?filter[status]=active&filter[category]=civil
GET /api/v2/laws?sort=-effectiveDate,title
```

### 5. WebSocket 实时推送

```
WS /api/v2/ws/imports/{taskId}
WS /api/v2/ws/search/{sessionId}
```

## v2 端点规划

### 法律资源 (/api/v2/laws)

| 方法 | 端点 | 描述 |
|------|------|------|
| GET | /api/v2/laws | 列表查询 (分页/过滤/排序) |
| GET | /api/v2/laws/{id} | 详情查询 |
| POST | /api/v2/laws | 创建法规 |
| PUT | /api/v2/laws/{id} | 更新法规 |
| DELETE | /api/v2/laws/{id} | 删除法规 |
| POST | /api/v2/laws/batch | 批量创建 |
| PATCH | /api/v2/laws/batch | 批量更新 |
| DELETE | /api/v2/laws/batch | 批量删除 |
| GET | /api/v2/laws/{id}/articles | 获取条款列表 |
| GET | /api/v2/laws/{id}/related | 获取关联法规 |

### 案例资源 (/api/v2/cases)

| 方法 | 端点 | 描述 |
|------|------|------|
| GET | /api/v2/cases | 列表查询 |
| GET | /api/v2/cases/{id} | 详情查询 |
| POST | /api/v2/cases | 创建案例 |
| PUT | /api/v2/cases/{id} | 更新案例 |
| DELETE | /api/v2/cases/{id} | 删除案例 |
| POST | /api/v2/cases/{id}/similar | 查找相似案例 |
| GET | /api/v2/cases/{id}/timeline | 获取案例时间线 |

### 搜索 (/api/v2/search)

| 方法 | 端点 | 描述 |
|------|------|------|
| GET | /api/v2/search | 统一搜索入口 |
| GET | /api/v2/search/laws | 法规搜索 |
| GET | /api/v2/search/cases | 案例搜索 |
| GET | /api/v2/search/suggest | 搜索建议 |
| POST | /api/v2/search/feedback | 搜索反馈 |

### 任务 (/api/v2/tasks)

| 方法 | 端点 | 描述 |
|------|------|------|
| GET | /api/v2/tasks | 任务列表 |
| GET | /api/v2/tasks/{id} | 任务详情 |
| DELETE | /api/v2/tasks/{id} | 取消任务 |
| GET | /api/v2/tasks/{id}/logs | 任务日志 |

## 实现计划

### Phase 1: 核心端点
- [ ] v2 基础框架 (Response 封装、分页)
- [ ] /api/v2/laws 端点
- [ ] /api/v2/cases 端点

### Phase 2: 高级功能
- [ ] 批量操作
- [ ] WebSocket 实时推送
- [ ] 过滤与排序

### Phase 3: 迁移
- [ ] API 文档更新
- [ ] 前端适配
- [ ] v1 → v2 迁移指南
