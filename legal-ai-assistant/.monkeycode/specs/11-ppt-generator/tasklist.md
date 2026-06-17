# Task List - PPT演讲稿生成器

## Phase 1: Backend Development

### Task 1.1 Database Schema
- [ ] 创建 `schema.sql` 中添加 `ppt_document` 表
- [ ] 添加 ppt_document 的索引

### Task 1.2 Backend API
- [ ] 创建 `PptController.java` - REST端点
- [ ] 创建 `PptService.java` - 业务逻辑
- [ ] 创建 `PPTGenerator.java` - AI生成PPT内容
- [ ] 创建 `PptxGenerator.java` - 使用POI生成PPTX文件
- [ ] 创建 `PptTemplateService.java` - 模板管理

### Task 1.3 DTOs
- [ ] 创建 `PptGenerateRequest.java`
- [ ] 创建 `PptGenerateResponse.java`
- [ ] 创建 `SlideDTO.java`
- [ ] 创建 `PptTemplateDTO.java`
- [ ] 创建 `PptDocumentDTO.java`

### Task 1.4 Repository
- [ ] 创建 `PptDocumentMapper.java`
- [ ] 创建 `PptDocumentRepository.java`

### Task 1.5 Testing
- [ ] 编写 PptService 单元测试
- [ ] 编写 PPTGenerator 单元测试

---

## Phase 2: Frontend Components

### Task 2.1 API Layer
- [ ] 在 `api/index.js` 添加PPT相关API方法
- [ ] 创建 `ppt.js` 单独管理PPT API

### Task 2.2 Store
- [ ] 创建 `store/ppt.js` Pinia状态管理

### Task 2.3 Components
- [ ] 创建 `views/PptEditor.vue` - PPT编辑器主界面
- [ ] 创建 `views/FileManager.vue` - PPT文件管理器
- [ ] 创建 `components/PptPreview.vue` - PPT预览组件
- [ ] 创建 `components/SlideEditor.vue` - 幻灯片编辑组件
- [ ] 创建 `components/TemplateSelector.vue` - 模板选择器

### Task 2.4 Router
- [ ] 在 `router/index.js` 添加 `/ppt-editor` 路由
- [ ] 在 `router/index.js` 添加 `/ppt-files` 路由
- [ ] 在 App.vue 添加菜单项

---

## Phase 3: Integration

### Task 3.1 LegalSearch Integration
- [ ] 在 `LegalSearch.vue` 添加"生成PPT"按钮
- [ ] 实现流式进度反馈UI
- [ ] 实现跳转到PPT编辑器

### Task 3.2 PPT Editor Features
- [ ] 实现幻灯片增删改
- [ ] 实现实时预览
- [ ] 实现模板切换预览
- [ ] 实现下载PPTX功能
- [ ] 实现保存到知识库功能

### Task 3.3 File Manager Features
- [ ] 实现文件列表展示
- [ ] 实现打开、下载、删除功能
- [ ] 实现重命名功能

### Task 3.4 UI Beautification
- [ ] 应用渐变色主题
- [ ] 添加圆角卡片样式
- [ ] 添加按钮动画效果
- [ ] 添加加载状态动画

---

## Phase 4: AI Template Recommendation (Optional)

### Task 4.1 AI Template Search
- [ ] 实现AI搜索网络模板API
- [ ] 实现模板预览图加载
- [ ] 实现模板下载和应用

---

## Estimated Timeline

| Phase | Duration | Key Deliverables |
|-------|----------|------------------|
| Phase 1 | 2 days | Backend API, PPTX generation |
| Phase 2 | 2 days | Frontend components |
| Phase 3 | 2 days | Integration, beautification |
| Phase 4 | 1 day | AI template recommendation |

**Total Estimated: 7 days**
