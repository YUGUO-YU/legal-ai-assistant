# Task List - PPT演讲稿生成器

## Phase 1: Backend Development

### Task 1.1 Database Schema
- [x] 创建 `schema.sql` 中添加 `ppt_document` 表
- [x] 添加 ppt_document 的索引

### Task 1.2 Backend API
- [x] 创建 `PptController.java` - REST端点
- [x] 创建 `PptService.java` - 业务逻辑
- [x] 创建 `PPTGenerator.java` - AI生成PPT内容
- [x] 创建 `PptxGenerator.java` - 使用POI生成PPTX文件
- [x] 创建 `PptTemplateService.java` - 模板管理

### Task 1.3 DTOs
- [x] 创建 `PptGenerateRequest.java`
- [x] 创建 `PptGenerateResponse.java`
- [x] 创建 `SlideDTO.java`
- [x] 创建 `PptTemplateDTO.java`
- [x] 创建 `PptDocumentDTO.java`

### Task 1.4 Repository
- [x] 创建 `PptDocumentMapper.java`
- [x] 创建 `PptDocumentRepository.java`

### Task 1.5 Testing
- [ ] 编写 PptService 单元测试
- [ ] 编写 PPTGenerator 单元测试

---

## Phase 2: Frontend Components

### Task 2.1 API Layer
- [x] 在 `api/index.js` 添加PPT相关API方法
- [x] 创建 `ppt.js` 单独管理PPT API

### Task 2.2 Store
- [x] 创建 `store/ppt.js` Pinia状态管理

### Task 2.3 Components
- [x] 创建 `views/PptEditor.vue` - PPT编辑器主界面
- [x] 创建 `views/FileManager.vue` - PPT文件管理器
- [x] 创建 `components/PptPreview.vue` - PPT预览组件
- [x] 创建 `components/SlideEditor.vue` - 幻灯片编辑组件
- [x] 创建 `components/TemplateSelector.vue` - 模板选择器

### Task 2.4 Router
- [x] 在 `router/index.js` 添加 `/ppt-editor` 路由
- [x] 在 `router/index.js` 添加 `/ppt-files` 路由
- [x] 在 App.vue 添加菜单项

---

## Phase 3: Integration

### Task 3.1 LegalSearch Integration
- [x] 在 `LegalSearch.vue` 添加"生成PPT"按钮
- [ ] 实现流式进度反馈UI
- [x] 实现跳转到PPT编辑器

### Task 3.2 PPT Editor Features
- [x] 实现幻灯片增删改
- [x] 实现实时预览
- [x] 实现模板切换预览
- [x] 实现下载PPTX功能
- [x] 实现保存到知识库功能

### Task 3.3 File Manager Features
- [x] 实现文件列表展示
- [x] 实现打开、下载、删除功能
- [x] 实现重命名功能

### Task 3.4 UI Beautification
- [x] 应用渐变色主题
- [x] 添加圆角卡片样式
- [x] 添加按钮动画效果
- [x] 添加加载状态动画

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
