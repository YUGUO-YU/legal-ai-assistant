# 法律AI助手 · 后台管理系统深度美化方案

Date: 2026-07-16
Status: Approved
Version: 1.0

---

## 1. Concept & Vision

法律AI助手后台管理系统定位为专业级 SaaS 管理控制台。视觉语言以「科技感深蓝/紫」为核心，采用毛玻璃（Glassmorphism）+ 渐变光效的现代设计手法，营造出沉浸、精致、高效的管理后台体验。所有界面元素服务于信息呈现与操作效率，视觉愉悦但绝不喧宾夺主。

---

## 2. Design Language

### 2.1 Color Palette

| Token | Hex | Usage |
|-------|-----|-------|
| `--color-primary` | `#667eea` | 主按钮、激活高亮 |
| `--color-primary-end` | `#764ba2` | 渐变终点 |
| `--color-accent` | `#818cf8` | Hover 光晕、次级强调 |
| `--color-bg-page` | `#0f0e1a` | 主区域背景（极深紫黑） |
| `--color-bg-sidebar` | `#13111c` | 侧边栏背景 |
| `--color-bg-card` | `rgba(30,27,75,0.6)` | 毛玻璃卡片背景 |
| `--color-bg-glass` | `rgba(255,255,255,0.05)` | 玻璃元素背景 |
| `--color-border-glass` | `rgba(255,255,255,0.1)` | 玻璃边框 |
| `--color-text-primary` | `#e2e8f0` | 主文字 |
| `--color-text-secondary` | `#94a3b8` | 次级文字 |
| `--color-text-muted` | `#64748b` | 占位/禁用文字 |
| `--color-success` | `#34d399` | 成功状态 |
| `--color-warning` | `#fbbf24` | 警告状态 |
| `--color-danger` | `#f87171` | 危险状态 |

### 2.2 Gradients

```
--gradient-primary: linear-gradient(135deg, #667eea 0%, #764ba2 100%)
--gradient-card: linear-gradient(135deg, rgba(102,126,234,0.15) 0%, rgba(118,75,162,0.15) 100%)
--gradient-glow: linear-gradient(135deg, rgba(102,126,234,0.3) 0%, rgba(118,75,162,0.3) 100%)
--gradient-text: linear-gradient(135deg, #667eea 0%, #a78bfa 50%, #764ba2 100%)
```

### 2.3 Glassmorphism System

```css
.glass {
  background: rgba(30, 27, 75, 0.6);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
}

.glass-light {
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 10px;
}
```

### 2.4 Shadows

```css
--shadow-glass: 0 8px 32px rgba(0, 0, 0, 0.4), inset 0 1px 0 rgba(255,255,255,0.05)
--shadow-glow: 0 0 24px rgba(102, 126, 234, 0.35)
--shadow-card: 0 4px 24px rgba(0, 0, 0, 0.3)
```

### 2.5 Typography

- 标题：`font-weight: 600`，使用渐变文字效果
- 正文：`font-size: 14px`，`line-height: 1.6`
- 辅助文字：`font-size: 12px`，`color: var(--color-text-secondary)`

### 2.6 Motion

| 动效 | 参数 |
|------|------|
| 页面淡入 | `0.4s ease-out`，`translateY(12px) → 0` |
| 卡片悬浮 | `0.25s ease`，`translateY(-4px)` + `shadow-glow` |
| 按钮点击 | `transform: scale(0.96)`，`0.15s` |
| 菜单展开 | `0.3s cubic-bezier(0.4, 0, 0.2, 1)` |
| 进度条 | 计数动画 `1.5s ease-out` |

---

## 3. Layout Structure

### 3.1 Sidebar (AdminLayout)

```
┌─────────────────────────────────────────────────────────┐
│ ┌──────────────┐                                        │
│ │  Legal AI    │  ← 渐变文字 Logo                       │
│ │  ⬡ Dashboard │  ← 毛玻璃背景侧边栏                    │
│ ├──────────────┤                                        │
│ │ 🔧 基础设施   │  ← 折叠子菜单                         │
│ │ 📋 法规管理   │                                        │
│ │ 💎 数据资产   │  ← 图标 + 渐变激活边框                │
│ │ 🤖 AI 能力   │                                        │
│ │ 📊 运营分析   │                                        │
│ │ 🔔 监控告警   │                                        │
│ │ ⚙️ 系统配置   │                                        │
│ ├──────────────┤                                        │
│ │ [折叠] [主题] │  ← 底部玻璃按钮组                    │
│ └──────────────┘                                        │
└─────────────────────────────────────────────────────────┘
```

- 宽度：`240px`（展开）/ `64px`（折叠）
- 背景：`rgba(19,17,28,0.95)` + `backdrop-filter: blur(24px)`
- 激活菜单项：左侧 `3px` 渐变紫色边框 + 背景微光
- Hover：背景 `rgba(255,255,255,0.06)` + 过渡 `0.2s`

### 3.2 Topbar

- 高度：`56px`
- 背景：半透明玻璃 `rgba(15,14,26,0.8)` + 底部 `1px` 微光线
- 标题：「Legal AI 后台」渐变文字
- 右侧：深色模式切换（玻璃按钮）+ 用户头像下拉

### 3.3 Page Layout

```
┌─ Breadcrumb ─────────────────────────────────────────┐
│ 首页 / 数据资产 / 法规主数据                           │
├─ Page Header ────────────────────────────────────────┤
│ [渐变大标题]                    [操作按钮组]          │
│ 描述文字                                               │
├─ Filter Card (玻璃) ────────────────────────────────┤
│ [ 搜索... ] [ 筛选 ] [ 重置 ]                        │
├─ Table Card (玻璃) ────────────────────────────────┤
│ ┌────┬────┬────┬────┬────┬─────┐                   │
│ │ ID │ 名称│ 状态│ 更新时间 │ 操作 │                   │
│ ├────┼────┼────┼────┼────┼─────┤                   │
│ │    │    │    │    │    │     │ ← 斑马纹 + hover  │
│ └────┴────┴────┴────┴────┴─────┘                   │
├─ Pagination ─────────────────────────────────────────┤
│                    [< 1 2 3 ... 10 >]                │
└──────────────────────────────────────────────────────┘
```

---

## 4. Component Specifications

### 4.1 KPI Card (Dashboard)

```
┌──────────────────────────────┐
│ ✦  活跃用户                  │  ← 线性图标 + 渐变色
│  1,234                       │  ← 大号数字，计数动画
│  ↑12% 较昨日                │  ← 次级文字 + 趋势标签
└──────────────────────────────┘
  ↓ hover: translateY(-4px) + shadow-glow
```

- 尺寸：`min-height: 100px`
- 背景：玻璃渐变 `rgba(102,126,234,0.08)` + 边框 `rgba(102,126,234,0.2)`
- 圆角：`12px`
- 悬浮：`translateY(-4px)` + `box-shadow: var(--shadow-glow)`

### 4.2 Data Table

- 容器：玻璃背景 + `12px` 圆角 + `overflow: hidden`
- 表头：`background: rgba(102,126,234,0.1)` + `font-weight: 600`
- 行：`background: transparent`，hover 时 `background: rgba(102,126,234,0.06)`
- 斑马纹：奇数行 `rgba(255,255,255,0.02)`
- 边框：`1px solid rgba(255,255,255,0.06)`
- 固定表头：`sticky top`

### 4.3 Filter Card

- 背景：`rgba(30,27,75,0.5)` + `backdrop-filter: blur(16px)`
- 边框：`1px solid rgba(255,255,255,0.08)`
- 圆角：`12px`
- 输入框：深色背景 `rgba(0,0,0,0.3)` + 紫色 focus 边框 `rgba(102,126,234,0.5)`

### 4.4 Action Buttons

- 主按钮：渐变背景 `--gradient-primary` + 白色文字 + hover 发光
- 次按钮：玻璃背景 + 边框 + hover 背景微光
- 危险按钮：`--color-danger` 渐变
- 文字按钮：hover 带下划线 + 颜色变化

### 4.5 Dialog/Modal

- 遮罩：`rgba(0,0,0,0.7)` + `backdrop-filter: blur(4px)`
- 面板：玻璃背景 + 渐变标题栏 + `16px` 圆角
- 标题：渐变文字或白色粗体
- 关闭按钮：右上角玻璃图标按钮

### 4.6 Pagination

- 背景：透明
- 当前页：渐变背景按钮
- 其他页：玻璃背景 hover
- 边框：`1px solid rgba(255,255,255,0.1)`

### 4.7 Tags/Badges

- 胶囊形状：`border-radius: 9999px`
- 状态标签：对应状态色渐变背景
- 模块标签：使用已有 `--color-purple/pink/cyan/lime` 等变量

---

## 5. Dark Mode Variables (Complete Override)

```css
[data-theme="dark"] {
  --color-bg-page: #0f0e1a;
  --color-bg-sidebar: #13111c;
  --color-bg-card: rgba(30,27,75,0.6);
  --color-bg-glass: rgba(255,255,255,0.05);
  --color-border-glass: rgba(255,255,255,0.08);
  --color-text-primary: #e2e8f0;
  --color-text-secondary: #94a3b8;
  --color-text-muted: #64748b;
  --color-primary: #667eea;
  --color-primary-light: #818cf8;
  --el-bg-color: #0f0e1a;
  --el-bg-color-overlay: #1e1b4b;
  --el-text-color-primary: #e2e8f0;
  --el-border-color: rgba(255,255,255,0.1);
  --el-fill-color-blank: #0f0e1a;
}
```

---

## 6. Page Header Standard

Every admin page must have this structure:

```html
<div class="admin-page">
  <div class="page-header">
    <div class="header-content">
      <h2>页面标题</h2>
      <p>页面描述 · 所属域</p>
    </div>
    <div class="header-actions">
      <el-button type="primary">操作按钮</el-button>
    </div>
  </div>

  <!-- 可选：筛选卡片 -->
  <el-card class="filter-card glass">...</el-card>

  <!-- 表格卡片 -->
  <el-card class="table-card glass">...</el-card>
</div>
```

```css
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}
.page-header h2 {
  font-size: 22px;
  font-weight: 600;
  background: var(--gradient-text);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  margin: 0 0 6px;
}
.page-header p {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin: 0;
}
```

---

## 7. Global Refinements

### 7.1 Custom Scrollbar
```css
::-webkit-scrollbar { width: 6px; height: 6px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb {
  background: rgba(102,126,234,0.3);
  border-radius: 3px;
}
::-webkit-scrollbar-thumb:hover { background: rgba(102,126,234,0.5); }
```

### 7.2 Text Selection
```css
::selection { background: rgba(102,126,234,0.4); color: #fff; }
```

### 7.3 Focus States
```css
*:focus-visible {
  outline: 2px solid rgba(102,126,234,0.6);
  outline-offset: 2px;
}
```

### 7.4 Skeleton Loading
```css
.skeleton {
  background: linear-gradient(90deg,
    rgba(102,126,234,0.08) 25%,
    rgba(102,126,234,0.15) 50%,
    rgba(102,126,234,0.08) 75%
  );
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}
```

---

## 8. Implementation Order

1. **全局 CSS 变量系统** — 毛玻璃、渐变、滚动条、selection
2. **AdminLayout** — 侧边栏毛玻璃、菜单样式、顶部栏
3. **Dashboard 首页** — KPI 卡片、图表、趋势图
4. **通用组件** — filter-card、table-card、pagination 样式
5. **所有 admin 页面** — 页面头部统一升级 + 表格/筛选器样式
6. **Element Plus 覆盖** — dialog、message、notification 毛玻璃风格

---

## 9. Deliverables

- `frontend/src/assets/styles/variables.css` — 完整毛玻璃 + 深色变量系统
- `frontend/src/assets/styles/animations.css` — 动效 + 骨架屏
- `frontend/src/assets/styles/glass.css` — 毛玻璃组件类（可选 import）
- `frontend/src/views/admin/AdminLayout.vue` — 重构侧边栏 + 顶部栏
- `frontend/src/views/admin/AdminDashboard.vue` — 全新 KPI + 图表设计
- 所有 `frontend/src/views/admin/**/` 页面 — 统一 page-header + 表格/筛选器
