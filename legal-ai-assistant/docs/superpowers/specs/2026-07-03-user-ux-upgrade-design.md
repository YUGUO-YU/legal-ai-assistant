# 用户端全局体验升级设计方案

## 1. 概述与目标

对 legal-ai-assistant 用户端进行全局体验升级，涵盖 CSS 变量主题系统、微交互与动效规范、重点页面视觉优化、以及基于昼夜时间的自动深色模式切换。

## 2. CSS 变量主题系统

### 2.1 变量规范

在 `frontend/src/assets/styles/variables.css` 建立全站统一的 CSS 变量体系：

```css
:root {
  --color-primary: #667eea;
  --color-primary-light: #818cf8;
  --color-primary-dark: #5568d3;
  --color-success: #10b981;
  --color-warning: #f59e0b;
  --color-danger: #ef4444;

  --color-bg: #ffffff;
  --color-bg-secondary: #f9fafb;
  --color-bg-card: #ffffff;
  --color-surface: #ffffff;

  --color-text: #1f2937;
  --color-text-secondary: #4b5563;
  --color-text-muted: #9ca3af;
  --color-text-inverse: #ffffff;

  --color-border: #e5e7eb;
  --color-divider: #f3f4f6;

  --radius-sm: 6px;
  --radius-md: 10px;
  --radius-lg: 14px;
  --radius-xl: 20px;

  --shadow-xs: 0 1px 3px rgba(0,0,0,0.06);
  --shadow-sm: 0 2px 8px rgba(0,0,0,0.08);
  --shadow-md: 0 4px 16px rgba(0,0,0,0.1);
  --shadow-lg: 0 8px 30px rgba(0,0,0,0.12);
  --shadow-glow: 0 0 20px rgba(102, 126, 234, 0.3);

  --transition-fast: 0.15s ease;
  --transition-base: 0.25s ease;
  --transition-smooth: 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  --transition-bounce: 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);

  --font-size-xs: 11px;
  --font-size-sm: 13px;
  --font-size-base: 14px;
  --font-size-lg: 16px;
  --font-size-xl: 18px;
  --font-size-2xl: 22px;

  --space-1: 4px;
  --space-2: 8px;
  --space-3: 12px;
  --space-4: 16px;
  --space-5: 20px;
  --space-6: 24px;
  --space-8: 32px;
}
```

### 2.2 深色模式（按昼夜时间自动切换）

**切换规则：**
- 白天 **06:00 - 18:00** → 亮色模式
- 夜晚 **18:00 - 次日 06:00** → 暗色模式

**实现方式：**
- 在 `App.vue` onMounted 时计算当前时间，判断应启用哪种主题
- 通过 `setTimeout` 在下次切换点自动切换（18:00 或 06:00）
- 主题状态存储于 `localStorage.themeMode`，页面刷新后恢复

**暗色配色：**
```css
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
}
```

## 3. 微交互与动效规范

### 3.1 全局动画类

```css
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

.fade-in {
  animation: fadeIn 0.3s ease both;
}

.stagger-item {
  animation: fadeInUp 0.36s ease-out both;
}
.stagger-item:nth-child(1) { animation-delay: 0.05s; }
.stagger-item:nth-child(2) { animation-delay: 0.1s; }
.stagger-item:nth-child(3) { animation-delay: 0.15s; }
.stagger-item:nth-child(4) { animation-delay: 0.2s; }
.stagger-item:nth-child(5) { animation-delay: 0.25s; }
.stagger-item:nth-child(6) { animation-delay: 0.3s; }
```

### 3.2 卡片悬停交互

```css
.card-hover {
  transition: transform var(--transition-base), box-shadow var(--transition-base);
}
.card-hover:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-lg);
}
```

### 3.3 按钮点击反馈

```css
.btn-press:active {
  transform: scale(0.96);
  transition: transform 0.1s;
}
```

### 3.4 搜索框焦点光晕

```css
.search-input:focus-within {
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.2);
  border-color: var(--color-primary);
}
```

### 3.5 结果展开过渡

```css
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
```

### 3.6 复制成功反馈

复制按钮点击后图标切换为 Check，1.5s 后恢复。

## 4. 重点页面改进

### 4.1 Dashboard

- 移除页面中的 emoji，改用 Element Plus 图标体系
- 统计卡片增加 `.card-hover` 悬停上浮效果
- 快捷入口网格增加 hover 层叠效果（scale + shadow）
- 热门话题进度条改为细线渐变样式（2px height）
- AI 状态指示灯保留脉冲动画，优化为暗色模式下颜色适配

### 4.2 LegalSearch

- 搜索框焦点增加左侧主题色左边框 + 光晕
- 结果卡片 hover 时左侧出现 3px 主题色 border
- 展开详情使用 `.result-expand` 过渡动画
- 复制按钮增加 `.btn-press` + checkmark 反馈
- 分页器样式统一为圆角风格

### 4.3 Document

- 模板卡片选中态：`scale(1.02)` + 主题色边框
- 生成按钮增加涟漪扩散效果（ripple）
- 标签页切换有下划线滑动指示器
- 表单分组之间增加 `.el-divider` 暗色模式适配

### 4.4 ContractReview

- 拖拽上传区虚线边框在 hover 时有动画虚线流动效果
- 风险等级进度条改为渐变色（高风险红→中风险黄→低风险绿）
- 分数圆形进度条在暗色模式下有外发光效果
- 结果区域入场使用 `fade-in-up` 动画

### 4.5 全局组件

- **Toast**：右侧新增关闭按钮，success/error/warning/info 四色边条适配暗色模式
- **Loading**：骨架屏 shimmer 动画替代旋转圆环，dark 模式下使用深色占位
- **EmptyState**：图标和文字在暗色模式下有正确的对比度

## 5. 实施步骤

1. 创建 `frontend/src/assets/styles/variables.css` 并在 `main.js` 中全局引入
2. 在 `App.vue` 实现主题初始化和定时切换逻辑
3. 为各页面组件添加统一的动效 class
4. 逐页面应用 CSS 变量替代硬编码颜色值
5. 优化各页面微交互细节
6. 验证深色模式在所有页面的正确渲染
