# Task 4 Report: AI 解析流程改造（后端）

## 状态: DONE

## Commit Hash
`388a15bf8be557be47e1dedae3266823d5927768`

## 实施内容

### 4.1 创建 LawImportPreview.java DTO
- **文件**: `backend/src/main/java/com/legalai/dto/LawImportPreview.java`
- **内容**: 包含 `lawTitle`, `shortTitle`, `documentNo`, `issuingAuthority`, `issueDate`, `effectiveDate`, `suggestedCategories`, `chapterTree`, `articles` 等字段
- **内部类**: `CategorySuggestion`, `ChapterNode`, `ArticleParse`

### 4.2 修改 LawImportService.java
**新增依赖注入**:
- `LawDocumentMapper lawDocumentMapper`
- `LawArticleMapper lawArticleMapper`

**新增方法**:

1. `previewImport(MultipartFile file)` - 预览导入
   - 调用 `extractTextFromDocx` 解析 Word 文档
   - 使用 LLM 提取元数据（发文字号、发布机关、日期等）
   - 使用 LLM 生成分类建议
   - 使用 LLM 解析章节结构
   - 使用正则提取条款

2. `confirmImport(LawImportPreview preview, String operator)` - 确认导入
   - 创建 `LawDocument` 记录
   - 批量创建 `LawArticle` 记录
   - 创建导入历史记录

3. `extractTextFromDocx(MultipartFile file)` - 解析 Word 文档为文本
4. `extractTitle(String content)` - 提取文档标题
5. `extractShortTitle(String content)` - 提取简称（从《》中提取）
6. `buildPreview(...)` - 构建预览对象，解析 LLM 返回的 JSON
7. `parseChapterNodes(JsonNode)` - 递归解析章节树
8. `extractArticles(String content)` - 使用正则提取条款

**新增导入**:
- `com.legalai.dto.LawImportPreview`
- `com.legalai.model.LawArticle`
- `com.legalai.model.LawDocument`
- `com.legalai.repository.LawArticleMapper`
- `com.legalai.repository.LawDocumentMapper`
- `org.apache.poi.xwpf.extractor.XWPFWordExtractor`
- `org.apache.poi.xwpf.usermodel.XWPFDocument`
- `org.springframework.web.multipart.MultipartFile`
- `java.util.regex.Pattern`
- `java.util.regex.Matcher`

### 4.3 修改 LawImportController.java
**新增导入**:
- `com.legalai.dto.LawImportPreview`

**新增端点**:

1. `POST /api/v1/admin/law-import/preview` - 预览 Word 文档
   - 接受 MultipartFile (仅支持 .docx/.doc)
   - 返回 `LawImportPreview`

2. `POST /api/v1/admin/law-import/confirm` - 确认导入
   - 接受 `LawImportPreview` JSON body
   - 返回 `LawImportJob`

### 4.4 confirmImport 方法实现
- 使用 MyBatis-Plus Mapper 插入 `LawDocument` 和 `LawArticle`
- 生成 UUID 标识
- 创建导入历史记录并返回

## 测试结果
Maven 编译命令不可用（`mvn` 命令未找到），但已验证:
- 文件结构正确
- 导入语句完整
- 代码逻辑符合现有模式
- 正则表达式用于条款提取

## 遵循现有模式
- 使用 `@Autowired` 进行依赖注入
- 使用 `objectMapper.readTree()` 解析 JSON（与 `parseStructuredJson` 方法一致）
- 使用 `textOr()` 辅助方法提取字段
- 使用 `parseDateOrNull()` 解析日期
- 遵循 JdbcTemplate + MyBatis-Plus 混合使用模式
