# Requirements Document - PPT演讲稿生成器

## Introduction

法律AI助手系统的PPT演讲稿生成模块（MOD-11），基于AI法律搜索结果，自动生成结构化PPT大纲，支持前端实时预览、用户编辑、模板选择，最终导出PPTX文件下载。

## Glossary

- **PPTX**: Microsoft PowerPoint Open XML Format，一种基于XML的演示文稿文件格式
- **模板主题**: PPT的视觉风格配置，包含配色方案、字体、背景等
- **幻灯片大纲**: PPT的页面结构和内容摘要
- **编辑模式**: 用户可修改AI生成的PPT内容的交互状态

## Requirements

### REQ-11.1 PPT大纲生成

**User Story:** 作为律师用户，我希望AI根据法律搜索结果自动生成PPT大纲，以便快速制作法律演示文稿。

#### Acceptance Criteria

1. WHEN用户在AI搜法页面点击"生成PPT"按钮，THEN系统 SHALL 根据当前搜索结果生成PPT大纲。
2. WHEN用户未进行搜索时点击"生成PPT"，THEN系统 SHALL 提示用户"请先进行法律搜索"。
3. WHENPPT生成过程中，THEN系统 SHALL 显示流式进度状态。
4. WHENPPT生成完成，THEN系统 SHALL 展示PPT大纲预览界面。

### REQ-11.2 PPT前端预览与编辑

**User Story:** 作为律师用户，我希望在前端实时预览PPT效果并编辑内容，以便定制化我的演示文稿。

#### Acceptance Criteria

1. WHENPPT大纲生成完成后，THEN系统 SHALL 在右侧面板渲染PPT预览。
2. WHEN用户点击编辑按钮，THEN系统 SHALL 进入编辑模式，允许修改标题、正文、备注。
3. WHEN用户修改内容时，THEN系统 SHALL 自动保存修改。
4. WHEN用户点击"添加幻灯片"，THEN系统 SHALL 在当前位置插入新的空白幻灯片。
5. WHEN用户点击"删除幻灯片"，THEN系统 SHALL 删除当前幻灯片并更新预览。

### REQ-11.3 模板选择

**User Story:** 作为律师用户，我希望选择不同的PPT模板风格，以适应不同场合的演示需求。

#### Acceptance Criteria

1. WHEN用户打开PPT编辑界面，THEN系统 SHALL 显示模板选择面板。
2. WHEN用户选择模板，THEN系统 SHALL 实时更新预览效果。
3. WHEN系统提供内置模板数量 >= 5种。
4. WHEN用户点击"AI推荐模板"，THEN系统 SHALL 调用AI搜索网络获取更多专业模板。
5. WHEN用户选择AI推荐模板，THEN系统 SHALL 下载并应用该模板。

### REQ-11.4 PPTX文件生成与下载

**User Story:** 作为律师用户，我希望下载标准PPTX文件，以便在本地使用PowerPoint或其他兼容软件打开。

#### Acceptance Criteria

1. WHEN用户点击"下载PPTX"按钮，THEN系统 SHALL 生成PPTX文件并触发浏览器下载。
2. WHEN用户点击"复制到知识库"，THEN系统 SHALL 将PPTX文件保存到用户知识库。
3. WHEN下载过程中，THEN系统 SHALL 显示下载进度。
4. WHEN文件生成失败，THEN系统 SHALL 显示错误提示并允许重试。

### REQ-11.5 文件管理功能

**User Story:** 作为律师用户，我希望管理已生成的PPT文件，以便查找和重复使用。

#### Acceptance Criteria

1. WHEN用户访问"文件管理"菜单，THEN系统 SHALL 显示已生成PPT文件的列表。
2. WHEN用户点击"打开"文件，THEN系统 SHALL 在PPT编辑器中打开该文件。
3. WHEN用户点击"下载"文件，THEN系统 SHALL 重新生成并下载PPTX文件。
4. WHEN用户点击"删除"文件，THEN系统 SHALL 显示确认对话框，THEN删除文件。
5. WHEN用户点击"重命名"文件，THEN系统 SHALL 显示重命名对话框。

### REQ-11.6 流式生成体验

**User Story:** 作为律师用户，我希望看到PPT生成的实时进度，以了解AI工作状态。

#### Acceptance Criteria

1. WHENPPT开始生成时，THEN系统 SHALL 显示流式进度条和当前步骤说明。
2. WHENAI生成每张幻灯片时，THEN系统 SHALL 动态高亮当前正在生成的幻灯片。
3. WHEN生成完成后，THEN系统 SHALL 显示"生成完成"提示并自动切换到预览视图。

## 内置模板列表

| 模板ID | 模板名称 | 主色调 | 适用场景 |
|--------|----------|--------|----------|
| TPL-01 | 法律蓝调 | #1a365d → #2c5282 | 正式法律论坛、学术演讲 |
| TPL-02 | 紫禁之巅 | #553c9a → #805ad5 | 高端商务演示 |
| TPL-03 | 专业沉稳 | #2d3748 → #4a5568 | 内部培训、工作汇报 |
| TPL-04 | 清新简约 | #319795 → #38b2ac | 案例分析分享 |
| TPL-05 | 法院灰金 | #744210 → #d69e2e | 司法研讨、学术交流 |

## User Interface Requirements

### REQ-11.7 UI设计要求

1. 系统 SHALL 使用渐变色主题 #667eea → #764ba2 作为主要强调色。
2. 所有卡片 SHALL 使用圆角边框（border-radius: 16px）。
3. 所有按钮 SHALL 支持hover状态动画。
4. 加载状态 SHALL 使用骨架屏或优雅的加载动画。

## Performance Requirements

### REQ-11.8 性能要求

1. PPT大纲生成 SHALL 在10秒内完成（Mock模式）。
2. PPTX文件生成 SHALL 在5秒内完成。
3. 前端预览 SHALL 支持实时编辑，响应延迟 <= 100ms。

## Error Handling

### REQ-11.9 错误处理

1. IF网络请求失败，THEN系统 SHALL 显示重试按钮。
2. IF文件生成失败，THEN系统 SHALL 记录错误日志并提示用户。
3. IF模板下载失败，THEN系统 SHALL 回退到上一个可用模板。

## Data Models

### PPTDocument

```json
{
  "id": "string",
  "title": "string",
  "slides": [
    {
      "id": "string",
      "layout": "string",
      "title": "string",
      "content": ["string"],
      "notes": "string",
      "background": "string"
    }
  ],
  "template": "string",
  "createdAt": "datetime",
  "updatedAt": "datetime"
}
```

### PPTTemplate

```json
{
  "id": "string",
  "name": "string",
  "thumbnail": "string",
  "styles": {
    "primaryColor": "string",
    "secondaryColor": "string",
    "fontFamily": "string",
    "background": "string"
  },
  "source": "local|ai"
}
```
