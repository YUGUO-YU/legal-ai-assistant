# 后台管理系统深度美化实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将法律AI助手后台管理系统升级为科技感深蓝/紫 SaaS 风格，涵盖侧边栏、Dashboard、所有管理页面的深度美化。

**Architecture:** 通过全面重构 `variables.css` 毛玻璃变量系统、重写 `AdminLayout.vue` 侧边栏和顶部栏、重新设计 `AdminDashboard.vue`，并为所有 45+ 个管理页面统一页面头部和通用组件样式，实现整体视觉升级。

**Tech Stack:** Vue 3 + Element Plus + SCSS + CSS Variables

---

## Global Constraints

- 主色调：`#667eea`（靛蓝紫）→ `#764ba2`（深紫）渐变
- 侧边栏背景：`#13111c`，主区域背景：`#0f0e1a`
- 毛玻璃：`backdrop-filter: blur(20px)` + `rgba(30,27,75,0.6)` 背景
- 深色模式：使用 `[data-theme="dark"]` 属性选择器（已确立）
- 动画时长：`0.25s ease` 常规 / `0.4s ease-out` 页面过渡
- 已有 CSS 变量必须保留兼容，新增变量加 `--color-` 前缀
- 所有颜色值使用 CSS 变量，不得硬编码十六进制值（渐变除外）

---

## File Inventory

### CSS 系统（全局样式）
- `frontend/src/assets/styles/variables.css` — 全面重构：毛玻璃变量、深色变量、渐变
- `frontend/src/assets/styles/animations.css` — 新增骨架屏、玻璃组件类、滚动条样式

### 布局组件
- `frontend/src/views/admin/AdminLayout.vue` — 重构侧边栏（毛玻璃、菜单发光、顶部栏渐变）
- `frontend/src/views/admin/AdminDashboard.vue` — 全新 KPI 卡片、图表主题、页面布局

### 通用样式（所有 admin 页面复用）
- 通过 `animations.css` 和 `variables.css` 提供，所有页面通过 class 引用

### 管理页面（统一 page-header + 表格样式）
以下 45+ 个页面全部应用统一美化：
- `infra/`: Users, FrontendUsers, Roles, Menus, AuditLogs, ServiceHealth, SearchFeedback, LawFavorites
- `law/`: LawCategoryTypes, LawCategories, LawRelations, LawImport, DataQuality
- `biz/`: Mod01Laws, Mod01Revisions, Mod01Crawl, Mod02Cases, Mod02Elements, Mod03Templates, Mod03Drafts, Mod03ReviewRules, Mod04Tasks, Mod05CompanyApis, Mod06CaseSearch, Mod07Laws, Mod08ContractRules, Mod09KbBases, Mod09Strategy, Mod10QaSessions
- `ai/`: Prompts, LlmModels, TokenUsage, MilvusCollections, KbChunks
- `ops/`: UserFeedback, SearchLogs, AppLogs
- `monitor/`: AlertRules, AlertHistory, Prometheus, AlertNotifications
- `sys/`: SysConfigs, SysDicts, Announcements, ApiEndpoints, DataDictionary, SystemMenus, AdminListPage

---

## Task Breakdown

### Task 1: 重构全局 CSS 变量系统

**Files:**
- Modify: `frontend/src/assets/styles/variables.css`

**Details:** 用深蓝/紫 SaaS 风格变量完全替换现有变量体系，保留原有 CSS 变量结构（移动端断点等），只替换颜色值。

- [ ] **Step 1: 备份现有 variables.css，创建新版本**

```css
/* 移动端断点 */
:root {
  --screen-xs: 480px;
  --screen-sm: 576px;
  --screen-md: 768px;
  --screen-lg: 992px;
  --screen-xl: 1200px;
  --screen-xxl: 1400px;

  /* ====== 深度美化：科技感深蓝/紫 SaaS 风格 ====== */

  /* Primary */
  --color-primary: #667eea;
  --color-primary-end: #764ba2;
  --color-primary-light: #818cf8;
  --color-primary-dark: #6366f1;

  /* Gradients */
  --gradient-primary: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  --gradient-text: linear-gradient(135deg, #667eea 0%, #a78bfa 50%, #764ba2 100%);
  --gradient-card: linear-gradient(135deg, rgba(102,126,234,0.12) 0%, rgba(118,75,162,0.12) 100%);
  --gradient-glow: linear-gradient(135deg, rgba(102,126,234,0.3) 0%, rgba(118,75,162,0.3) 100%);

  /* Page Background */
  --color-bg-page: #0f0e1a;
  --color-bg-sidebar: #13111c;
  --color-bg-card: rgba(30, 27, 75, 0.6);
  --color-bg-glass: rgba(255, 255, 255, 0.05);
  --color-bg-glass-hover: rgba(255, 255, 255, 0.08);
  --color-bg-overlay: rgba(0, 0, 0, 0.7);

  /* Glass Borders */
  --color-border-glass: rgba(255, 255, 255, 0.08);
  --color-border-glass-hover: rgba(255, 255, 255, 0.15);

  /* Status */
  --color-success: #34d399;
  --color-warning: #fbbf24;
  --color-danger: #f87171;
  --color-info: #818cf8;

  /* Text */
  --color-text-primary: #e2e8f0;
  --color-text-secondary: #94a3b8;
  --color-text-muted: #64748b;
  --color-text-placeholder: #475569;
  --color-text-inverse: #ffffff;

  /* Module Colors (保留) */
  --color-purple: #8b5cf6;
  --color-pink: #ec4899;
  --color-cyan: #06b6d4;
  --color-lime: #84cc16;
  --color-indigo: #6366f1;
  --color-teal: #14b8a6;
  --color-orange: #f97316;

  /* Shadows */
  --shadow-glass: 0 8px 32px rgba(0, 0, 0, 0.4), inset 0 1px 0 rgba(255, 255, 255, 0.05);
  --shadow-glow: 0 0 24px rgba(102, 126, 234, 0.35);
  --shadow-card: 0 4px 24px rgba(0, 0, 0, 0.3);
  --shadow-sm: 0 2px 8px rgba(0, 0, 0, 0.3);
  --shadow-md: 0 4px 16px rgba(0, 0, 0, 0.4);
  --shadow-lg: 0 8px 24px rgba(0, 0, 0, 0.5);

  /* Transitions */
  --transition-fast: 0.15s ease;
  --transition-base: 0.25s ease;
  --transition-smooth: 0.35s cubic-bezier(0.4, 0, 0.2, 1);

  /* Border Radius */
  --radius-sm: 6px;
  --radius-md: 10px;
  --radius-lg: 14px;
  --radius-xl: 18px;
  --radius-full: 9999px;

  /* Spacing */
  --space-1: 4px;
  --space-2: 8px;
  --space-3: 12px;
  --space-4: 16px;
  --space-5: 20px;
  --space-6: 24px;
  --space-8: 32px;
  --spacing-xs: 8px;
  --spacing-sm: 12px;
  --spacing-md: 16px;
  --spacing-lg: 24px;
  --spacing-xl: 32px;
}

/* Dark Mode 完整覆盖 */
[data-theme="dark"] {
  --color-bg-page: #0f0e1a;
  --color-bg-secondary: #13111c;
  --color-bg-card: rgba(30, 27, 75, 0.6);
  --color-bg-glass: rgba(255, 255, 255, 0.05);
  --color-border-glass: rgba(255, 255, 255, 0.08);
  --color-text-primary: #e2e8f0;
  --color-text-secondary: #94a3b8;
  --color-text-muted: #64748b;

  /* Element Plus 覆盖 */
  --el-bg-color: #0f0e1a;
  --el-bg-color-page: #0a0d12;
  --el-bg-color-overlay: #1e1b4b;
  --el-text-color-primary: #e2e8f0;
  --el-text-color-regular: #94a3b8;
  --el-text-color-secondary: #64748b;
  --el-border-color: rgba(255, 255, 255, 0.1);
  --el-border-color-light: rgba(255, 255, 255, 0.07);
  --el-border-color-lighter: rgba(255, 255, 255, 0.05);
  --el-fill-color-blank: #0f0e1a;
  --el-mask-color: rgba(0, 0, 0, 0.8);
  --el-color-primary: #667eea;
  --el-color-success: #34d399;
  --el-color-warning: #fbbf24;
  --el-color-danger: #f87171;
}
```

- [ ] **Step 2: 验证文件语法正确**

Run: 在编辑器中检查 SCSS 语法，确保所有括号匹配

- [ ] **Step 3: 提交**

```bash
git add frontend/src/assets/styles/variables.css
git commit -m "feat(admin): overhaul CSS variables for deep blue/purple SaaS glassmorphism theme"
```

---

### Task 2: 扩展 animations.css 全局样式

**Files:**
- Modify: `frontend/src/assets/styles/animations.css`

**Details:** 在现有文件末尾追加玻璃组件类、滚动条、text-selection、骨架屏、对话框覆盖等全局样式。

- [ ] **Step 1: 追加全局样式到 animations.css**

在文件末尾添加：

```css
/* ======================
   毛玻璃组件类
   ====================== */
.glass {
  background: var(--color-bg-card);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--color-border-glass);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-glass);
}

.glass-light {
  background: var(--color-bg-glass);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid var(--color-border-glass);
  border-radius: var(--radius-md);
}

.glass-hover:hover {
  border-color: var(--color-border-glass-hover);
  box-shadow: var(--shadow-glow);
}

/* ======================
   全局滚动条样式
   ====================== */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}
::-webkit-scrollbar-track {
  background: transparent;
}
::-webkit-scrollbar-thumb {
  background: rgba(102, 126, 234, 0.25);
  border-radius: 3px;
}
::-webkit-scrollbar-thumb:hover {
  background: rgba(102, 126, 234, 0.45);
}

/* ======================
   文本选中颜色
   ====================== */
::selection {
  background: rgba(102, 126, 234, 0.4);
  color: #fff;
}

/* ======================
   Focus 可访问性
   ====================== */
*:focus-visible {
  outline: 2px solid rgba(102, 126, 234, 0.6);
  outline-offset: 2px;
}

/* ======================
   渐变文字
   ====================== */
.gradient-text {
  background: var(--gradient-text);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

/* ======================
   Element Plus Dialog 毛玻璃
   ====================== */
.el-dialog {
  border-radius: var(--radius-xl) !important;
  border: 1px solid var(--color-border-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}
.el-dialog__header {
  border-bottom: 1px solid var(--color-border-glass);
  padding: 16px 20px;
  margin-right: 0 !important;
}
.el-dialog__title {
  font-weight: 600;
  color: var(--color-text-primary);
}
.el-dialog__body {
  color: var(--color-text-primary);
}
.el-overlay {
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
}

/* ======================
   Element Plus Message 毛玻璃
   ====================== */
.el-message {
  border-radius: var(--radius-md) !important;
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid var(--color-border-glass) !important;
  background: rgba(30, 27, 75, 0.9) !important;
}

/* ======================
   Element Plus Notification
   ====================== */
.el-notification {
  border-radius: var(--radius-lg) !important;
  border: 1px solid var(--color-border-glass);
  background: rgba(30, 27, 75, 0.95) !important;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}

/* ======================
   Element Plus 按钮渐变
   ====================== */
.el-button--primary {
  background: var(--gradient-primary) !important;
  border: none !important;
  box-shadow: 0 4px 14px rgba(102, 126, 234, 0.35);
}
.el-button--primary:hover {
  box-shadow: var(--shadow-glow) !important;
  transform: translateY(-1px);
}
.el-button--primary:active {
  transform: scale(0.97);
}

/* ======================
   Element Plus Card 毛玻璃
   ====================== */
.el-card {
  border-radius: var(--radius-lg) !important;
  border: 1px solid var(--color-border-glass) !important;
  background: var(--color-bg-card) !important;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  box-shadow: var(--shadow-card) !important;
}

/* ======================
   Element Plus Table 玻璃风格
   ====================== */
.el-table {
  --el-table-border-color: rgba(255, 255, 255, 0.06);
  --el-table-header-bg-color: rgba(102, 126, 234, 0.1);
  --el-table-row-hover-bg-color: rgba(102, 126, 234, 0.06);
  --el-table-tr-bg-color: transparent;
  background-color: transparent !important;
}
.el-table th.el-table__cell {
  background-color: rgba(102, 126, 234, 0.1) !important;
  font-weight: 600;
  color: var(--color-text-primary);
}
.el-table td.el-table__cell {
  border-bottom: 1px solid rgba(255, 255, 255, 0.04) !important;
}
.el-table__body tr:hover > td.el-table__cell {
  background: rgba(102, 126, 234, 0.08) !important;
}
.el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell {
  background: rgba(255, 255, 255, 0.02);
}

/* ======================
   Element Plus Input/Select 深色
   ====================== */
.el-input__wrapper,
.el-textarea__inner {
  background: rgba(0, 0, 0, 0.3) !important;
  border: 1px solid var(--color-border-glass) !important;
  box-shadow: none !important;
  border-radius: var(--radius-md) !important;
}
.el-input__wrapper:focus-within,
.el-textarea__inner:focus {
  border-color: rgba(102, 126, 234, 0.5) !important;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.15) !important;
}
.el-select .el-input__wrapper {
  border-radius: var(--radius-md) !important;
}

/* ======================
   Element Plus Pagination
   ====================== */
.el-pagination {
  --el-pagination-button-bg-color: transparent;
  --el-pagination-hover-color: var(--color-primary);
}
.el-pagination .el-pager li {
  background: var(--color-bg-glass);
  border: 1px solid var(--color-border-glass);
  border-radius: var(--radius-sm);
  color: var(--color-text-secondary);
}
.el-pagination .el-pager li.is-active {
  background: var(--gradient-primary);
  border-color: transparent;
  color: #fff;
}
.el-pagination .el-pager li:hover {
  color: var(--color-primary-light);
}

/* ======================
   骨架屏 shimmer
   ====================== */
@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}
.skeleton {
  background: linear-gradient(
    90deg,
    rgba(102, 126, 234, 0.08) 25%,
    rgba(102, 126, 234, 0.15) 50%,
    rgba(102, 126, 234, 0.08) 75%
  );
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: var(--radius-sm);
}

/* ======================
   统计卡片悬浮上浮
   ====================== */
.kpi-card-hover {
  transition: transform var(--transition-base), box-shadow var(--transition-base);
  cursor: pointer;
}
.kpi-card-hover:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-glow);
}

/* ======================
   表格卡片
   ====================== */
.table-card {
  overflow: hidden;
}
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/assets/styles/animations.css
git commit -m "feat(admin): add glassmorphism classes, scrollbars, Element Plus overrides to animations.css"
```

---

### Task 3: 重构 AdminLayout 侧边栏和顶部栏

**Files:**
- Modify: `frontend/src/views/admin/AdminLayout.vue`

**Details:** 重构侧边栏为深色毛玻璃风格，菜单激活态加渐变边框和发光效果，顶部栏加渐变文字，整体过渡动画流畅。

- [ ] **Step 1: 重写 AdminLayout.vue**

替换整个 `<style>` 块为以下 SCSS（保留 template 和 script 不变）：

```scss
<style lang="scss" scoped>
.admin-layout {
  display: flex;
  min-height: 100vh;
  background: var(--color-bg-page);
}

// ======== 侧边栏 ========
.aside {
  width: 240px;
  min-height: 100vh;
  background: rgba(19, 17, 28, 0.95);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border-right: 1px solid rgba(255, 255, 255, 0.06);
  display: flex;
  flex-direction: column;
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  z-index: 1000;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);

  &--collapsed {
    width: 64px;
  }
}

.aside-header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  flex-shrink: 0;

  .logo-text {
    font-size: 17px;
    font-weight: 700;
    background: var(--gradient-text);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    white-space: nowrap;
    letter-spacing: 0.5px;
  }

  .collapse-icon {
    cursor: pointer;
    color: var(--color-text-muted);
    font-size: 16px;
    padding: 6px;
    border-radius: var(--radius-sm);
    transition: all var(--transition-fast);

    &:hover {
      background: var(--color-bg-glass-hover);
      color: var(--color-text-primary);
    }
  }
}

.aside-menu {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  border-right: none !important;
  padding: 8px 0;

  // 通用菜单项
  .el-menu-item,
  .el-sub-menu__title {
    border-radius: var(--radius-md);
    margin: 2px 8px;
    padding-left: 12px !important;
    height: 40px;
    line-height: 40px;
    color: var(--color-text-secondary);
    transition: all var(--transition-fast);

    .el-icon {
      color: var(--color-text-muted);
      transition: color var(--transition-fast);
    }

    &:hover {
      background: rgba(255, 255, 255, 0.06) !important;
      color: var(--color-text-primary) !important;

      .el-icon {
        color: var(--color-primary-light);
      }
    }
  }

  // 激活态
  .el-menu-item.is-active {
    background: linear-gradient(90deg, rgba(102, 126, 234, 0.2) 0%, rgba(102, 126, 234, 0.08) 100%) !important;
    border-left: 3px solid transparent;
    border-image: linear-gradient(180deg, #667eea, #764ba2) 1;
    color: var(--color-primary-light) !important;
    font-weight: 600;

    .el-icon {
      color: var(--color-primary-light);
      filter: drop-shadow(0 0 6px rgba(102, 126, 234, 0.6));
    }
  }

  // 子菜单标题
  .el-sub-menu__title {
    &:hover {
      background: rgba(255, 255, 255, 0.06) !important;
    }
  }

  // 子菜单弹出
  .el-menu--inline {
    .el-menu-item {
      padding-left: 44px !important;
      font-size: 13px;
    }
  }
}

// 折叠时菜单
.aside--collapsed {
  .aside-header {
    justify-content: center;
    padding: 0 8px;

    .logo-text { display: none; }
    .collapse-icon { margin: 0; }
  }

  .el-menu--collapse {
    width: 100% !important;
  }

  .el-menu-item,
  .el-sub-menu__title {
    justify-content: center;
    padding-left: 0 !important;
    padding-right: 0 !important;

    span, .el-sub-menu__icon-arrow { display: none !important; }
  }
}

// ======== 主内容区 ========
.right-area {
  flex: 1;
  margin-left: 240px;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  transition: margin-left 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.aside--collapsed ~ .right-area {
  margin-left: 64px;
}

// ======== 顶部栏 ========
.top-bar {
  height: 60px;
  background: rgba(15, 14, 26, 0.85);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  position: sticky;
  top: 0;
  z-index: 100;

  .top-bar-title {
    font-size: 15px;
    font-weight: 600;
    background: var(--gradient-text);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }

  .top-bar-right {
    display: flex;
    align-items: center;
    gap: 8px;

    .theme-toggle {
      width: 36px;
      height: 36px;
      border-radius: var(--radius-md);
      background: var(--color-bg-glass);
      border: 1px solid var(--color-border-glass);
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      color: var(--color-text-secondary);
      transition: all var(--transition-fast);

      &:hover {
        background: var(--color-bg-glass-hover);
        border-color: var(--color-border-glass-hover);
        color: var(--color-primary-light);
        box-shadow: var(--shadow-glow);
      }
    }

    .user-avatar {
      width: 34px;
      height: 34px;
      border-radius: 50%;
      background: var(--gradient-primary);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 13px;
      font-weight: 600;
      color: #fff;
      cursor: pointer;
    }

    .user-name {
      font-size: 13px;
      color: var(--color-text-secondary);
      font-weight: 500;
    }
  }
}

// ======== 主内容 ========
.main {
  flex: 1;
  padding: 20px 24px;
  background: var(--color-bg-page);
}

.page-breadcrumb {
  margin-bottom: 16px;

  .el-breadcrumb__inner {
    color: var(--color-text-muted);
    font-size: 13px;
  }
  .el-breadcrumb__inner.is-link:hover {
    color: var(--color-primary-light);
  }
  .el-breadcrumb__separator {
    color: var(--color-text-muted);
  }
}

// ======== 页面过渡动画 ========
.admin-fade-enter-active {
  animation: adminPageFadeIn 0.4s ease-out;
}
.admin-fade-leave-active {
  animation: adminPageFadeOut 0.2s ease-in;
}
@keyframes adminPageFadeIn {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}
@keyframes adminPageFadeOut {
  from { opacity: 1; transform: translateY(0); }
  to { opacity: 0; transform: translateY(-8px); }
}

// ======== 移动端 ========
.mobile-header {
  display: none;
  height: 56px;
  background: rgba(19, 17, 28, 0.95);
  backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  padding: 0 16px;
  align-items: center;
  gap: 12px;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1001;

  @media (max-width: 768px) {
    display: flex;
  }

  .mobile-menu-btn {
    font-size: 20px;
    cursor: pointer;
    color: var(--color-text-secondary);
    display: flex;
    align-items: center;
    justify-content: center;
    width: 40px;
    height: 40px;
    border-radius: var(--radius-md);
    transition: all var(--transition-fast);

    &:hover {
      background: var(--color-bg-glass);
      color: var(--color-text-primary);
    }
  }

  .mobile-title {
    font-weight: 600;
    font-size: 16px;
    color: var(--color-text-primary);
  }
}

.sidebar-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  z-index: 999;

  @media (min-width: 768px) { display: none; }
}

.admin-layout.is-mobile {
  .aside {
    position: fixed;
    top: 0;
    left: 0;
    bottom: 0;
    z-index: 1000;
    transform: translateX(-100%);
    transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);

    &.is-open {
      transform: translateX(0);
    }
  }

  .right-area {
    margin-left: 0 !important;
  }

  .main {
    padding-top: 72px;
  }
}

// ======== 侧边栏底部按钮 ========
.sidebar-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  background: rgba(0, 0, 0, 0.2);
  flex-shrink: 0;

  .footer-btn {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 8px 12px;
    border-radius: var(--radius-md);
    background: var(--color-bg-glass);
    border: 1px solid var(--color-border-glass);
    cursor: pointer;
    color: var(--color-text-muted);
    font-size: 12px;
    transition: all var(--transition-fast);
    flex: 1;
    justify-content: center;

    .el-icon { font-size: 15px; }

    &:hover {
      background: var(--color-bg-glass-hover);
      border-color: var(--color-border-glass-hover);
      color: var(--color-primary-light);
      box-shadow: var(--shadow-glow);
    }
  }
}

.aside--collapsed .sidebar-footer {
  flex-direction: column;
  gap: 4px;
  padding: 12px 8px;

  .footer-btn {
    width: 44px;
    height: 40px;
    padding: 0;
    flex: none;
    justify-content: center;
  }
}
</style>
```

- [ ] **Step 2: 同时修改 template 中的 logo 文字**

将侧边栏顶部的 `<span v-if="!sidebarCollapsed">法律AI助手</span>` 改为 `<span v-if="!sidebarCollapsed" class="logo-text">LegalAI</span>`

- [ ] **Step 3: 追加全局深色覆盖样式（在 `</style>` 后加 `<style>` 不带 scoped）**

```html
<style>
/* 全局深色菜单覆盖 */
.el-menu {
  background: transparent !important;
  border: none !important;
}
.el-sub-menu .el-menu--inline {
  background: transparent !important;
}
.el-popper.is-dark {
  background: rgba(30, 27, 75, 0.95) !important;
  border: 1px solid var(--color-border-glass) !important;
  backdrop-filter: blur(16px);
}
</style>
```

- [ ] **Step 4: 构建验证**

Run: `cd frontend && npm run build 2>&1 | tail -20`

- [ ] **Step 5: 提交**

```bash
git add frontend/src/views/admin/AdminLayout.vue
git commit -m "feat(admin): redesign sidebar with glassmorphism, gradient logo, glowing menu items"
```

---

### Task 4: 重新设计 AdminDashboard 首页

**Files:**
- Modify: `frontend/src/views/admin/AdminDashboard.vue`

**Details:** 重新设计 Dashboard 的 KPI 卡片、图表区域、页面布局，采用毛玻璃卡片 + 渐变边框 + 悬浮发光效果。

- [ ] **Step 1: 重写 KPI 卡片区域的 template**

找到 `<el-row :gutter="14" class="kpi-row">` 块（大约第 28-36 行），替换为：

```html
<div class="kpi-grid">
  <div v-for="(m, index) in kpis" :key="m.label" class="kpi-card glass kpi-card-hover" :class="m.tone">
    <div class="kpi-icon-wrap">
      <el-icon class="kpi-icon"><component :is="m.icon || 'Odometer'" /></el-icon>
    </div>
    <div class="kpi-body">
      <div class="kpi-label">{{ m.label }}</div>
      <div class="kpi-value">{{ animatedKpis[index]?.value ?? 0 }}</div>
      <div class="kpi-foot">{{ m.foot }}</div>
    </div>
  </div>
</div>
```

- [ ] **Step 2: 追加 KPI 卡片 SCSS 样式（在现有 `<style>` 块中替换旧的 `.kpi-row` 相关样式）**

替换 `.kpi-row` 相关样式为：

```scss
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}

.kpi-card {
  padding: 18px 20px;
  display: flex;
  align-items: flex-start;
  gap: 14px;
  cursor: pointer;

  &.tone-purple .kpi-icon-wrap { background: rgba(139, 92, 246, 0.15); color: #a78bfa; }
  &.tone-green .kpi-icon-wrap { background: rgba(52, 211, 153, 0.15); color: #34d399; }
  &.tone-orange .kpi-icon-wrap { background: rgba(249, 115, 22, 0.15); color: #fb923c; }
  &.tone-blue .kpi-icon-wrap { background: rgba(96, 165, 250, 0.15); color: #60a5fa; }
  &.tone-pink .kpi-icon-wrap { background: rgba(236, 72, 153, 0.15); color: #f472b6; }
  &.tone-default .kpi-icon-wrap { background: rgba(102, 126, 234, 0.15); color: #818cf8; }

  .kpi-icon-wrap {
    width: 42px;
    height: 42px;
    border-radius: var(--radius-md);
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;

    .kpi-icon {
      font-size: 20px;
    }
  }

  .kpi-body {
    flex: 1;
    min-width: 0;
  }

  .kpi-label {
    font-size: 12px;
    color: var(--color-text-muted);
    margin-bottom: 6px;
    font-weight: 500;
  }

  .kpi-value {
    font-size: 26px;
    font-weight: 700;
    color: var(--color-text-primary);
    line-height: 1.2;
    margin-bottom: 4px;
  }

  .kpi-foot {
    font-size: 11px;
    color: var(--color-text-muted);
  }
}

.page-header {
  margin-bottom: 20px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 12px;

  h2 {
    font-size: 22px;
    font-weight: 600;
    margin: 0 0 6px;
    background: var(--gradient-text);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }

  p {
    margin: 0;
    font-size: 13px;
    color: var(--color-text-muted);
  }

  .header-actions {
    display: flex;
    gap: 10px;
    align-items: center;
  }
}

.db-alert {
  margin-bottom: 16px;
  border-radius: var(--radius-lg) !important;
  border: 1px solid rgba(251, 191, 36, 0.3) !important;
  background: rgba(251, 191, 36, 0.08) !important;
}

.activity-row {
  margin-bottom: 14px;

  > .el-col > .el-card {
    height: 100%;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.chart-area {
  height: 220px;
  position: relative;

  .chart-overlay {
    position: absolute;
    bottom: 8px;
    right: 8px;
    font-size: 11px;
    color: var(--color-text-muted);
    background: rgba(0,0,0,0.3);
    padding: 2px 8px;
    border-radius: var(--radius-sm);
  }
}

.activity-kpis {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  text-align: center;

  .activity-kpi-label {
    font-size: 11px;
    color: var(--color-text-muted);
    margin-bottom: 6px;
  }

  .activity-kpi-value {
    font-size: 22px;
    font-weight: 700;
    color: var(--color-text-primary);
  }
}

// 响应式 KPI
@media (max-width: 768px) {
  .kpi-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .activity-kpis {
    grid-template-columns: repeat(3, 1fr);
    .activity-kpi-value { font-size: 16px; }
  }
}
```

- [ ] **Step 3: 提交**

```bash
git add frontend/src/views/admin/AdminDashboard.vue
git commit -m "feat(admin): redesign Dashboard with glassmorphism KPI cards, gradient headers, hover effects"
```

---

### Task 5: 批量升级所有 admin 页面的 page-header

**Files:**
- Modify: 全部 45+ 个 `frontend/src/views/admin/**/` 下的 `.vue` 文件

**Details:** 为每个页面统一添加 class="admin-page"，确保 `<div class="page-header">` 结构完整，加载 `animations.css` 中的 `.glass` 类到表格卡片和筛选区域。

**执行方式：** 使用 subagent 或批量脚本逐个/分批处理，每个子任务处理一个模块（infra, law, biz, ai, ops, monitor, sys），保持每个模块内的 commit 原子性。

- [ ] **Step 1: 检查每个页面的 page-header 结构**

对每个页面检查：
- 是否有 `class="page-header"` 或 `class="admin-page-header"` 包装
- h2 标题是否使用了 CSS 变量
- 操作按钮区域是否在 `.header-actions` 内
- 表格卡片是否有 `class="table-card"` + `class="glass"`
- 筛选区域是否有 `class="filter-card glass"`

- [ ] **Step 2: 统一修改每个页面**

每个页面需要的修改（以 Users.vue 为例）：
1. 在最外层 div 添加 `class="admin-page"`
2. 将 page-header 的 h2 改为渐变文字（加 `class="gradient-text"` 或内联 gradient style）
3. 给表格卡片加 `class="glass table-card"`
4. 给筛选 form 所在 el-card 加 `class="glass filter-card"`
5. 确保 pagination 容器有 `class="pagination-container"`

通用模式：

```html
<div class="admin-page">
  <div class="page-header">
    <div class="header-content">
      <h2 class="gradient-text">页面标题</h2>
      <p>描述 · 域</p>
    </div>
    <div class="header-actions">
      <el-button type="primary">操作</el-button>
    </div>
  </div>

  <el-card class="glass filter-card" style="margin-bottom: 14px;">
    <!-- 筛选表单 -->
  </el-card>

  <el-card class="glass table-card">
    <!-- 表格 -->
    <template #footer>
      <div class="pagination-container">
        <el-pagination ... />
      </div>
    </template>
  </el-card>
</div>
```

- [ ] **Step 3: 分模块提交**

每完成一个模块（如 infra 的所有页面）即单独提交：

```bash
git add frontend/src/views/admin/infra/
git commit -m "feat(admin): apply glassmorphism page-header and card styles to infra pages"
```

---

## 实施顺序建议

| 顺序 | 任务 | 说明 |
|------|------|------|
| 1 | Task 1: CSS 变量系统 | 全局基础，必须最先完成 |
| 2 | Task 2: animations.css 扩展 | 全局组件类，紧跟 Task 1 |
| 3 | Task 3: AdminLayout 重构 | 侧边栏是最重要的视觉元素 |
| 4 | Task 4: Dashboard 重设计 | 首页是用户第一眼 |
| 5 | Task 5: 批量页面升级 | 45+ 页面的标准化工序 |

---

## 验收标准

- [ ] `npm run build` 无报错
- [ ] 侧边栏毛玻璃效果可见，菜单激活态有紫色渐变边框
- [ ] Dashboard KPI 卡片有悬浮上浮 + 光晕效果
- [ ] 所有管理页面 page-header 标题使用渐变文字
- [ ] 表格卡片使用玻璃背景 + 圆角
- [ ] 深色模式下所有样式正常（无白底/透明背景问题）
- [ ] 滚动条为紫色细条
- [ ] Element Plus Dialog / Message / Notification 为毛玻璃风格
- [ ] 每个 commit 后 push 到 origin/main
