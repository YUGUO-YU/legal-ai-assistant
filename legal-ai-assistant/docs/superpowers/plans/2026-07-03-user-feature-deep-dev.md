# 用户端功能深度开发实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 对用户端三个核心功能模块进行深度开发：① Dashboard 接入真实数据 ② DocQa 会话增强 ③ 企业查询增强。

**Tech Stack:** Vue 3 + Element Plus + JavaScript, 前端在 `/workspace/legal-ai-assistant/frontend/`

---

## Global Constraints

- 所有 API 调用使用已有的 `api` 实例（`import api from '../api'` 或 `@/api`）
- 保持与现有 UI 风格一致，使用 CSS 变量（来自 `variables.css`）
- 所有页面保持响应式布局
- loading 状态使用 Element Plus 的 `v-loading` 指令
- 错误处理使用 `ElMessage.error()` 提示
- 构建验证：`cd /workspace/legal-ai-assistant/frontend && npm run build`

---

## Part 1: Dashboard 数据化

### 现状
- Dashboard 全部为静态展示数据（hardcoded 数字）
- 无 API 调用，无真实数据加载

### 目标
- 页面加载时从后端 API 获取统计数据
- 展示真实的检索次数、会话数、活跃天数等
- 保留静态骨架屏加载态

### Task 1.1: 添加 Dashboard 数据加载

**Files:**
- Modify: `frontend/src/views/Dashboard.vue`

**Interfaces:**
- 新增 API: `GET /api/v1/user/stats`（如果后端不存在则模拟返回数据）
- 响应格式: `{ searchCount, sessionCount, activeDays, efficiencyRate, recentActivities[] }`

**Steps:**
- [ ] 在 `Dashboard.vue` 的 `<script setup>` 中添加 `statsLoading` ref
- [ ] 添加 `loadStats()` 函数，调用 `fetch('/api/v1/user/stats')`
- [ ] 如果 API 不存在（404），使用模拟数据确保 UI 正常
- [ ] 在 `onMounted` 中调用 `loadStats()`
- [ ] 为统计卡片添加 `v-loading="statsLoading"`
- [ ] 将硬编码的 `statsData` 改为从 API 响应读取

---

## Part 2: DocQa 会话增强

### 现状
- DocQa 有基础会话功能（上传文档、提问、历史会话）
- 缺少错误处理 UI、追问建议机制、上下文记忆增强

### 目标
- 增加错误提示 UI（网络错误、超时、API 异常）
- 增加会话续接功能（从历史会话恢复）
- 追问建议卡片（根据上下文推荐 3 个追问方向）

### Task 2.1: DocQa 错误处理 UI

**Files:**
- Modify: `frontend/src/views/DocQa.vue`

**Steps:**
- [ ] 在模板中添加错误提示区域（`v-if="errorMsg"`）
- [ ] 在 `fetchAnswer` 函数中 catch 块设置 `errorMsg`
- [ ] 错误提示使用 `ElMessage.error()` + 页面内联错误框双保险
- [ ] 添加"重试"按钮触发重新请求

### Task 2.2: DocQa 历史会话恢复

**Files:**
- Modify: `frontend/src/views/DocQa.vue`

**Steps:**
- [ ] 在会话历史列表中，每个会话项增加"继续对话"按钮
- [ ] 点击后加载该会话的所有消息记录到当前对话
- [ ] 使用 `sessionId` 追踪当前会话

### Task 2.3: DocQa 追问建议

**Files:**
- Modify: `frontend/src/views/DocQa.vue`

**Steps:**
- [ ] 在答案区域底部添加"追问建议"区域
- [ ] 根据用户问题内容，动态生成 3 个推荐追问（前端模拟或调用小模型）
- [ ] 建议项使用 `el-tag` 可点击，点击后自动填充输入框

---

## Part 3: 企业查询增强

### 现状
- CompanyQuery 有基础查询功能（输入企业名、查询结果、风险等级）
- 进度动画简陋、API 集成不完整、结果展示较简单

### 目标
- 优化查询进度动画（增加步骤指示器）
- 丰富企业信息展示（工商信息、司法风险、经营状况）
- 增加查询历史记录（localStorage 存储）

### Task 3.1: CompanyQuery 进度动画优化

**Files:**
- Modify: `frontend/src/views/CompanyQuery.vue`

**Steps:**
- [ ] 将当前简陋的加载状态改为分步骤进度指示
- [ ] 进度步骤：输入验证 → API查询 → 数据分析 → 结果展示
- [ ] 每个步骤有图标和文字说明，完成后打勾
- [ ] 使用 `el-steps` 或自定义 CSS 动画实现

### Task 3.2: CompanyQuery 丰富信息展示

**Files:**
- Modify: `frontend/src/views/CompanyQuery.vue`

**Steps:**
- [ ] 查询结果区域增加 Tab 切换：基本信息 / 司法风险 / 经营状况
- [ ] 基本信息展示：注册资本、成立日期、经营范围、法定代表人
- [ ] 司法风险展示：失信被执行、法律诉讼数量统计
- [ ] 经营状况展示：商标、专利资质、招聘趋势

### Task 3.3: CompanyQuery 查询历史

**Files:**
- Modify: `frontend/src/views/CompanyQuery.vue`

**Steps:**
- [ ] 使用 `localStorage.companyQueryHistory` 存储最近 10 条查询
- [ ] 在页面顶部展示历史查询列表
- [ ] 点击历史项可快速复现查询
- [ ] 每次新查询成功后写入历史记录

---

## 实施顺序

1. Task 1.1: Dashboard 数据加载
2. Task 2.1: DocQa 错误处理
3. Task 2.2: DocQa 历史会话恢复
4. Task 2.3: DocQa 追问建议
5. Task 3.1: CompanyQuery 进度动画
6. Task 3.2: CompanyQuery 信息展示
7. Task 3.3: CompanyQuery 查询历史
