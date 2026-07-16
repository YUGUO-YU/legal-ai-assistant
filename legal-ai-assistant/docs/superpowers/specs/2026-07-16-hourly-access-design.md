# 用户访问量可视化 - 24小时访问趋势

## 1. 概述

在后台管理仪表盘（AdminDashboard）新增 **24小时访问趋势图**，以小时为单位展示当日访问量分布，帮助管理员了解用户访问高峰时段。

## 2. 后端改动

### 2.1 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/admin/stats/hourly-access` | 返回今日逐小时访问量 |

### 2.2 响应格式

```json
{
  "code": 0,
  "data": {
    "today": [12, 8, 3, 1, 0, 2, 9, 34, 67, 89, 76, 54, ...],  // 24个数字，索引0=0点
    "yesterday": [10, 5, 2, 1, 0, 1, 7, 30, 60, 80, 70, 50, ...]
  }
}
```

### 2.3 实现位置

- `AdminController.java` — 新增 `@GetMapping("/stats/hourly-access")`
- `AdminDataService.java` — 新增 `getHourlyAccess()` 方法

### 2.4 SQL

```sql
-- 今日各小时访问量
SELECT HOUR(login_at) AS hour, COUNT(*) AS count
FROM user_login_history
WHERE DATE(login_at) = CURDATE()
GROUP BY HOUR(login_at)

-- 昨日各小时访问量
SELECT HOUR(login_at) AS hour, COUNT(*) AS count
FROM user_login_history
WHERE DATE(login_at) = DATE_SUB(CURDATE(), INTERVAL 1 DAY)
GROUP BY HOUR(login_at)
```

结果按 0~23 小时补齐，未访问的小时返回 count=0。

## 3. 前端改动

### 3.1 组件位置

在 AdminDashboard 的 `用户活跃度趋势（近7天）` 卡片下方，新增一个 `el-col` 卡片。

### 3.2 数据层

```js
// ref
const hourlyAccess = ref({ today: Array(24).fill(0), yesterday: Array(24).fill(0) })

// loadHourlyAccess()
async function loadHourlyAccess() {
  const res = await api.get('/admin/stats/hourly-access')
  hourlyAccess.value = res?.data || { today: Array(24).fill(0), yesterday: Array(24).fill(0) }
}

// loadAll() 中调用
loadHourlyAccess()
```

### 3.3 图表配置

- 类型：柱状图（bar）+ 折线图（line）叠加
- X轴：0~23 时
- Y轴：访问次数
- 系列1（柱）：今日访问量，使用 `adminChartPalette[0]` 紫色
- 系列2（线）：昨日访问量，使用半透明灰线，虚线
- tooltip：显示具体小时和次数

### 3.4 样式

沿用现有 `glass card` + `card-header` 样式，标题为"今日访问趋势（24小时）"。

## 4. 验收标准

- [ ] 后端接口返回今日24小时数据，未访问小时 count=0
- [ ] 前端图表正确渲染24根柱状
- [ ] 页面 loading 时显示骨架屏
- [ ] `npm run build` 通过
- [ ] 提交并推送 GitHub
