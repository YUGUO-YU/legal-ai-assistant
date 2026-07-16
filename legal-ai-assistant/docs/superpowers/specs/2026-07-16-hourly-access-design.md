# 实施计划：24小时访问趋势

## 后端实现

- [ ] 1. AdminController.java 新增 `/stats/hourly-access` 接口
  - 参考现有 `@GetMapping("/stats/user-activity")` 的写法
  - 调用 `adminDataService.getHourlyAccess()`
  - 返回 `ApiResponse<Map<String, Object>>`

- [ ] 2. AdminDataService.java 新增 `getHourlyAccess()` 方法
  - 查询今日 0~23 时各小时登录次数（SQL: `SELECT HOUR(login_at), COUNT(*) FROM user_login_history WHERE DATE(login_at) = CURDATE() GROUP BY HOUR(login_at)`）
  - 查询昨日同期数据（`DATE_SUB(CURDATE(), INTERVAL 1 DAY)`）
  - 返回 `Map<String, Object>` 含 `today` 和 `yesterday` 两个 int[24] 数组
  - 未出现的小时补 0

## 前端实现

- [ ] 3. AdminDashboard.vue 添加 `hourlyAccess` ref
  - 初始值 `{ today: Array(24).fill(0), yesterday: Array(24).fill(0) }`

- [ ] 4. AdminDashboard.vue 添加 `loadHourlyAccess()` 函数
  - 调用 `api.get('/admin/stats/hourly-access')`
  - 写入 `hourlyAccess.value`

- [ ] 5. AdminDashboard.vue 在 `loadAll()` 中调用 `loadHourlyAccess()`

- [ ] 6. AdminDashboard.vue 添加骨架屏
  - 在现有"用户活跃度趋势（近7天）"卡片下方新增 `el-col` 卡片
  - loading 时显示 shimmer 骨架图（参考 KPI grid 骨架写法）

- [ ] 7. AdminDashboard.vue 添加 `hourlyAccessOption` computed
  - 柱状图（bar）：今日访问量，使用 `adminChartPalette[0]`
  - 折线图（line）：昨日访问量，虚线，半透明
  - X轴：0~23 时，Y轴：访问次数
  - tooltip 显示"X时：N次"

- [ ] 8. AdminDashboard.vue 模板新增图表卡片
  - `el-card > card-header` 标题"今日访问趋势（24小时）"
  - `<v-chart :option="hourlyAccessOption" autoresize />`

## 验证

- [ ] 9. `cd backend && mvn compile` 通过
- [ ] 10. `cd frontend && npm run build` 通过
- [ ] 11. 提交并推送
