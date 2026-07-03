# 用户端全局体验升级实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 对 legal-ai-assistant 用户端进行全局体验升级，包含 CSS 变量主题系统、昼夜自动切换深色模式、微交互动效、以及重点页面（Dashboard/LegalSearch/Document/ContractReview）视觉优化。

**Architecture:** 在 `frontend/src/assets/styles/` 建立 `variables.css` 全局变量文件，在 `App.vue` 实现基于时间的深色模式切换，各页面通过 CSS 变量实现主题适配，通过统一的动效 class 实现微交互。

**Tech Stack:** Vue 3 + Element Plus + SCSS

---

## Global Constraints

- 暗色模式切换时间点：白天 06:00-18:00 为亮色，18:00-次日 06:00 为暗色
- 主题偏好存储于 `localStorage.themeMode`
- 所有颜色值必须通过 CSS 变量引用，禁止硬编码
- 动画时长：`fast=0.15s`，`base=0.25s`，`smooth=0.35s`，`bounce=0.4s`

---

## Task Map

| Task | 内容 | 文件 |
|------|------|------|
| 1 | CSS 变量主题系统 | `frontend/src/assets/styles/variables.css`（新建） |
| 2 | 深色模式切换逻辑 | `frontend/src/App.vue` |
| 3 | 全局动画样式 | `frontend/src/assets/styles/animations.css`（新建） |
| 4 | Dashboard 动效与布局优化 | `frontend/src/views/Dashboard.vue` |
| 5 | LegalSearch 微交互优化 | `frontend/src/views/LegalSearch.vue` |
| 6 | Document 动效优化 | `frontend/src/views/Document.vue` |
| 7 | ContractReview 动效优化 | `frontend/src/views/ContractReview.vue` |
| 8 | Toast/Loading/EmptyState 组件优化 | `frontend/src/components/` |
| 9 | 全局变量引入 | `frontend/src/main.js` |

---

## Task 1: 创建 CSS 变量主题系统

**Files:**
- Create: `frontend/src/assets/styles/variables.css`

**Interfaces:**
- Produces: 全站 CSS 变量（`--color-*`, `--shadow-*`, `--radius-*`, `--transition-*` 等）

- [ ] **Step 1: 创建 variables.css 文件**

```css
/* frontend/src/assets/styles/variables.css */

:root {
  /* Primary Colors */
  --color-primary: #667eea;
  --color-primary-light: #818cf8;
  --color-primary-dark: #5568d3;

  /* Status Colors */
  --color-success: #10b981;
  --color-warning: #f59e0b;
  --color-danger: #ef4444;
  --color-info: #3b82f6;

  /* Light Mode Background */
  --color-bg: #ffffff;
  --color-bg-secondary: #f9fafb;
  --color-bg-card: #ffffff;
  --color-surface: #ffffff;

  /* Light Mode Text */
  --color-text: #1f2937;
  --color-text-secondary: #4b5563;
  --color-text-muted: #9ca3af;
  --color-text-inverse: #ffffff;

  /* Light Mode Border */
  --color-border: #e5e7eb;
  --color-divider: #f3f4f6;

  /* Border Radius */
  --radius-sm: 6px;
  --radius-md: 10px;
  --radius-lg: 14px;
  --radius-xl: 20px;

  /* Shadows */
  --shadow-xs: 0 1px 3px rgba(0,0,0,0.06);
  --shadow-sm: 0 2px 8px rgba(0,0,0,0.08);
  --shadow-md: 0 4px 16px rgba(0,0,0,0.1);
  --shadow-lg: 0 8px 30px rgba(0,0,0,0.12);
  --shadow-glow: 0 0 20px rgba(102, 126, 234, 0.3);

  /* Transitions */
  --transition-fast: 0.15s ease;
  --transition-base: 0.25s ease;
  --transition-smooth: 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  --transition-bounce: 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);

  /* Font Sizes */
  --font-size-xs: 11px;
  --font-size-sm: 13px;
  --font-size-base: 14px;
  --font-size-lg: 16px;
  --font-size-xl: 18px;
  --font-size-2xl: 22px;

  /* Spacing */
  --space-1: 4px;
  --space-2: 8px;
  --space-3: 12px;
  --space-4: 16px;
  --space-5: 20px;
  --space-6: 24px;
  --space-8: 32px;
}

/* Dark Mode */
[data-theme="dark"] {
  --color-bg: #0f172a;
  --color-bg-secondary: #1e293b;
  --color-bg-card: #1e293b;
  --color-surface: #1e293b;

  --color-text: #f1f5f9;
  --color-text-secondary: #cbd5e1;
  --color-text-muted: #64748b;

  --color-border: #334155;
  --color-divider: #1e293b;

  --shadow-sm: 0 2px 8px rgba(0,0,0,0.3);
  --shadow-md: 0 4px 16px rgba(0,0,0,0.4);
  --shadow-lg: 0 8px 30px rgba(0,0,0,0.5);
  --shadow-glow: 0 0 24px rgba(129, 140, 248, 0.25);

  --color-primary: #818cf8;
  --color-primary-light: #a5b4fc;
  --color-primary-dark: #6366f1;

  --color-success: #34d399;
  --color-warning: #fbbf24;
  --color-danger: #f87171;
  --color-info: #60a5fa;
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/assets/styles/variables.css
git commit -m "feat(frontend): add CSS variables theme system"
```

---

## Task 2: 在 App.vue 实现深色模式自动切换

**Files:**
- Modify: `frontend/src/App.vue`

**Interfaces:**
- Consumes: `localStorage.themeMode`
- Produces: `data-theme` attribute on `<html>` element

- [ ] **Step 1: 读取现有 App.vue 结构**

```bash
head -50 frontend/src/App.vue
```

- [ ] **Step 2: 在 App.vue 的 `<script setup>` 中添加主题初始化逻辑**

```javascript
// App.vue <script setup>
import { onMounted, onUnmounted } from 'vue'

const THEME_KEY = 'themeMode'

function isDaytime() {
  const hour = new Date().getHours()
  return hour >= 6 && hour < 18
}

function getInitialTheme() {
  const saved = localStorage.getItem(THEME_KEY)
  if (saved === 'dark' || saved === 'light') return saved
  return isDaytime() ? 'light' : 'dark'
}

function applyTheme(theme) {
  document.documentElement.setAttribute('data-theme', theme)
  localStorage.setItem(THEME_KEY, theme)
}

let themeTimer = null

function scheduleNextThemeSwitch() {
  if (themeTimer) clearTimeout(themeTimer)
  const now = new Date()
  const hour = now.getHours()
  let msUntilNext

  if (isDaytime()) {
    // Currently day, switch to dark at 18:00
    const switchTime = new Date(now)
    switchTime.setHours(18, 0, 0, 0)
    msUntilNext = switchTime - now
  } else {
    // Currently night, switch to light at 06:00
    const switchTime = new Date(now)
    switchTime.setHours(6, 0, 0, 0)
    if (switchTime <= now) switchTime.setDate(switchTime.getDate() + 1)
    msUntilNext = switchTime - now
  }

  themeTimer = setTimeout(() => {
    const nextTheme = document.documentElement.getAttribute('data-theme') === 'dark' ? 'light' : 'dark'
    applyTheme(nextTheme)
    scheduleNextThemeSwitch()
  }, msUntilNext)
}

onMounted(() => {
  const theme = getInitialTheme()
  applyTheme(theme)
  scheduleNextThemeSwitch()
})

onUnmounted(() => {
  if (themeTimer) clearTimeout(themeTimer)
})
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/App.vue
git commit -m "feat(frontend): add time-based auto dark/light mode switching"
```

---

## Task 3: 创建全局动画样式文件

**Files:**
- Create: `frontend/src/assets/styles/animations.css`

**Interfaces:**
- Produces: 全局动画类（`.fade-in-up`, `.card-hover`, `.btn-press`, `.stagger-item` 等）

- [ ] **Step 1: 创建 animations.css**

```css
/* frontend/src/assets/styles/animations.css */

/* 淡入上移动画 */
.fade-in-up {
  animation: fadeInUp 0.36s ease-out both;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 淡入动画 */
.fade-in {
  animation: fadeIn 0.3s ease both;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

/* 卡片悬停上浮 */
.card-hover {
  transition: transform var(--transition-base), box-shadow var(--transition-base);
}
.card-hover:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-lg);
}

/* 按钮点击缩放反馈 */
.btn-press {
  transition: transform var(--transition-fast);
}
.btn-press:active {
  transform: scale(0.96);
}

/* 交错入场动画 */
.stagger-item {
  animation: fadeInUp 0.36s ease-out both;
}
.stagger-item:nth-child(1) { animation-delay: 0.05s; }
.stagger-item:nth-child(2) { animation-delay: 0.1s; }
.stagger-item:nth-child(3) { animation-delay: 0.15s; }
.stagger-item:nth-child(4) { animation-delay: 0.2s; }
.stagger-item:nth-child(5) { animation-delay: 0.25s; }
.stagger-item:nth-child(6) { animation-delay: 0.3s; }
.stagger-item:nth-child(7) { animation-delay: 0.35s; }
.stagger-item:nth-child(8) { animation-delay: 0.4s; }

/* 搜索框焦点光晕 */
.search-input-glow:focus-within {
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.2);
  border-color: var(--color-primary) !important;
}

/* 结果卡片悬停左边框 */
.result-card-hover {
  transition: border-color var(--transition-base), box-shadow var(--transition-base);
  border-left: 3px solid transparent;
}
.result-card-hover:hover {
  border-left-color: var(--color-primary);
  box-shadow: var(--shadow-sm);
}

/* 展开收起动画 */
.result-expand-enter-active {
  transition: all 0.3s ease;
  overflow: hidden;
}
.result-expand-leave-active {
  transition: all 0.25s ease;
  overflow: hidden;
}
.result-expand-enter-from,
.result-expand-leave-to {
  opacity: 0;
  max-height: 0;
}
.result-expand-enter-to,
.result-expand-leave-from {
  opacity: 1;
  max-height: 1000px;
}

/* 涟漪效果 */
.ripple-effect {
  position: relative;
  overflow: hidden;
}
.ripple-effect::after {
  content: '';
  position: absolute;
  width: 100%;
  height: 100%;
  top: 0;
  left: 0;
  pointer-events: none;
  background-image: radial-gradient(circle, var(--color-primary) 10%, transparent 10%);
  background-repeat: no-repeat;
  background-position: 50%;
  transform: scale(10);
  opacity: 0;
  transition: transform 0.5s, opacity 0.5s;
}
.ripple-effect:active::after {
  transform: scale(0);
  opacity: 0.3;
  transition: 0s;
}

/* 虚线边框流动动画（用于拖拽上传区） */
@keyframes dashFlow {
  to {
    stroke-dashoffset: -12;
  }
}
.dash-flow {
  background-image: repeating-linear-gradient(
    90deg,
    var(--color-border) 0,
    var(--color-border) 6px,
    transparent 6px,
    transparent 12px
  );
  animation: dashFlow 0.8s linear infinite;
}

/* 骨架屏 shimmer */
@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}
.skeleton {
  background: linear-gradient(
    90deg,
    var(--color-bg-secondary) 25%,
    var(--color-border) 50%,
    var(--color-bg-secondary) 75%
  );
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: var(--radius-sm);
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/assets/styles/animations.css
git commit -m "feat(frontend): add global animation styles"
```

---

## Task 4: Dashboard 动效与布局优化

**Files:**
- Modify: `frontend/src/views/Dashboard.vue`
- Modify: `frontend/src/assets/styles/variables.css`（追加暗色模式适配）

**Interfaces:**
- Consumes: `.card-hover`, `.stagger-item`, `.fade-in-up` 等动画类
- Produces: 更流畅的 Dashboard 动效体验

- [ ] **Step 1: 移除 Dashboard 中的 emoji**

搜索 `👋` 并替换为 Element Plus 图标或纯文字

- [ ] **Step 2: 为 stat-card 添加 card-hover 类**

在 `stat-card` 的 class 中添加 `card-hover`

- [ ] **Step 3: 为 quick-item 添加 card-hover 类**

在 `quick-item` 的 class 中添加 `card-hover`

- [ ] **Step 4: 为 detail-card 添加 card-hover 类**

在 `detail-card` 的 class 中添加 `card-hover`

- [ ] **Step 5: 为热门话题进度条添加样式**

将 hotTopics 进度条 height 从默认改为 3px，添加渐变色

- [ ] **Step 6: 为最近活动项添加 stagger-item 类**

在 `recentActivities` 列表项中添加 `.stagger-item` class

- [ ] **Step 7: Commit**

```bash
git add frontend/src/views/Dashboard.vue
git commit -m "feat(frontend): enhance Dashboard with micro-interactions and hover effects"
```

---

## Task 5: LegalSearch 微交互优化

**Files:**
- Modify: `frontend/src/views/LegalSearch.vue`

**Interfaces:**
- Consumes: `.search-input-glow`, `.result-card-hover`, `.result-expand`, `.btn-press`

- [ ] **Step 1: 为搜索框容器添加 search-input-glow 类**

在 `.search-input-wrapper` 添加 class `search-input-glow`

- [ ] **Step 2: 为结果卡片添加 result-card-hover 类**

在结果列表容器中的卡片添加 class `result-card-hover`

- [ ] **Step 3: 为结果展开动画添加 transition-group**

将结果区域用 `<transition-group name="result-expand">` 包裹

- [ ] **Step 4: 为复制按钮添加 btn-press 类**

在所有复制图标按钮的 class 中添加 `btn-press`

- [ ] **Step 5: 添加复制成功反馈逻辑**

在 `copyContent` 函数中，复制成功后图标临时切换为 Check，1.5s 后恢复

```javascript
const copyContent = async (item) => {
  const text = `${item.lawTitle} ${item.articleNo} ${item.title}\n${item.content}`
  await navigator.clipboard.writeText(text)
  ElMessage.success('已复制到剪贴板')
  // 可选：添加图标反馈
}
```

- [ ] **Step 6: Commit**

```bash
git add frontend/src/views/LegalSearch.vue
git commit -m "feat(frontend): enhance LegalSearch with micro-interactions"
```

---

## Task 6: Document 动效优化

**Files:**
- Modify: `frontend/src/views/Document.vue`

**Interfaces:**
- Consumes: `.btn-press`, `.ripple-effect`

- [ ] **Step 1: 为模板卡片添加选中态样式**

在选中模板时添加 `scale(1.02)` + `border: 2px solid var(--color-primary)` 效果

- [ ] **Step 2: 为生成按钮添加 ripple-effect 类**

在 `handleDraft` 按钮上添加 `ripple-effect btn-press` 类

- [ ] **Step 3: 为标签页切换添加下划线指示器动画**

通过 CSS 添加标签页下划线滑动效果

- [ ] **Step 4: Commit**

```bash
git add frontend/src/views/Document.vue
git commit -m "feat(frontend): enhance Document with template selection animation"
```

---

## Task 7: ContractReview 动效优化

**Files:**
- Modify: `frontend/src/views/ContractReview.vue`

**Interfaces:**
- Consumes: `.dash-flow`, `.fade-in-up`, `.ripple-effect`

- [ ] **Step 1: 为拖拽上传区添加 dash-flow 动画**

在 drag upload 区域的 border style 中添加虚线动画

- [ ] **Step 2: 为风险等级进度条添加渐变色**

将高/中/低风险进度条改为渐变色（高风险: `#ef4444 → #f87171`，中风险: `#f59e0b → #fbbf24`，低风险: `#10b981 → #34d399`）

- [ ] **Step 3: 为审查结果区域添加 fade-in-up 动画**

在 `reviewResult` 显示的区域添加 `fade-in-up` class

- [ ] **Step 4: Commit**

```bash
git add frontend/src/views/ContractReview.vue
git commit -m "feat(frontend): enhance ContractReview with drag animation and gradient progress"
```

---

## Task 8: Toast / Loading / EmptyState 组件优化

**Files:**
- Modify: `frontend/src/components/Toast.vue`（如存在）
- Modify: `frontend/src/components/Loading.vue`
- Modify: `frontend/src/components/EmptyState.vue`

**Interfaces:**
- Consumes: CSS 变量
- Produces: 适配暗色模式的组件样式

- [ ] **Step 1: Loading.vue - 骨架屏替代旋转圆环**

在 Loading.vue 中新增 skeleton 模式的 Loading，保留 spin 模式作为备选

- [ ] **Step 2: EmptyState.vue - 暗色模式适配**

检查图标和文字颜色是否通过 CSS 变量引用，确保暗色模式下对比度正确

- [ ] **Step 3: Toast.vue - 右侧关闭按钮和暗色模式适配**

如果 Toast.vue 存在，添加右侧关闭按钮，并确保颜色变量化

- [ ] **Step 4: Commit**

```bash
git add frontend/src/components/Loading.vue frontend/src/components/EmptyState.vue
git commit -m "feat(frontend): enhance global components with dark mode support"
```

---

## Task 9: 在 main.js 中全局引入样式文件

**Files:**
- Modify: `frontend/src/main.js`

**Interfaces:**
- Consumes: `variables.css`, `animations.css`
- Produces: 全局 CSS 变量和动画类可用

- [ ] **Step 1: 在 main.js 顶部添加样式引入**

```javascript
import '@/assets/styles/variables.css'
import '@/assets/styles/animations.css'
```

- [ ] **Step 2: 验证构建通过**

```bash
cd frontend && npm run build
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/main.js
git commit -m "feat(frontend): import global CSS variables and animations"
```

---

## 实施完成后验证清单

- [ ] `npm run build` 构建成功，无报错
- [ ] 亮色模式下所有页面颜色正常
- [ ] 暗色模式下所有页面对比度正确（文字可读、卡片有边界）
- [ ] 动效流畅，无卡顿
- [ ] 深色模式在 06:00 和 18:00 自动切换正常
