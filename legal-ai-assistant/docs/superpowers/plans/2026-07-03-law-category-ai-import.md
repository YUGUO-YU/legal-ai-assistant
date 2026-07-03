# 法规分类管理与 AI 一键导入实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 新增法规多维度分类体系（分类维度表 + 分类项表 + 关联表），改造 `law_document` 接入分类外键，在 AdminController 中新增分类管理接口，并开发独立的 Admin 前端页面和改造导入流程支持 AI 解析预览确认。

**Architecture:**
- 后端：MyBatis Plus ORM + Apache POI（Word 解析）+ LLMClient（AI 分类+章节结构解析）
- 前端：Vue 3 + Element Plus，Admin 侧边栏新增「法规分类管理」菜单
- 数据库：新增 3 张表（law_category_type / law_category / law_document_category）

**Tech Stack:** Spring Boot + MyBatis Plus + Apache POI + Vue 3 + Element Plus

---

## Global Constraints

- 所有后端新增类放在 `com.legalai.admin` 包下（admin 相关功能）或 `com.legalai` 下（原有位置）
- Controller 路由统一前缀 `/api/v1/admin/law`
- 前端新增页面放在 `frontend/src/views/admin/biz/` 下
- 数据库表名：`law_category_type`, `law_category`, `law_document_category`
- API 返回格式统一用 `ApiResponse<T>`
- 前端使用已有的 API 实例模式：`import api from '@/api'` 或从 `../api`

---

## Task 1: 数据库表设计与实体类创建

### 1.1 创建 SQL 迁移脚本

**Files:**
- Create: `backend/src/main/resources/migration/V2026_07_03__law_category_system.sql`

```sql
-- 分类维度字典（按层级/部门/行业分组）
CREATE TABLE IF NOT EXISTS law_category_type (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  type_code VARCHAR(32) NOT NULL UNIQUE COMMENT '维度代码: level/department/industry',
  type_name VARCHAR(64) NOT NULL COMMENT '维度名称',
  description VARCHAR(256) COMMENT '维度描述',
  sort_order INT DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分类维度字典';

-- 具体分类项（支持多级树形）
CREATE TABLE IF NOT EXISTS law_category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  category_type_id BIGINT NOT NULL COMMENT '关联维度ID',
  parent_id BIGINT DEFAULT NULL COMMENT '父级分类ID（支持多级）',
  category_code VARCHAR(64) NOT NULL UNIQUE COMMENT '分类编码',
  category_name VARCHAR(128) NOT NULL COMMENT '分类名称',
  color VARCHAR(16) DEFAULT '#667eea' COMMENT '前端展示颜色',
  sort_order INT DEFAULT 0,
  status TINYINT DEFAULT 1 COMMENT '1=启用 0=禁用',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (category_type_id) REFERENCES law_category_type(id),
  FOREIGN KEY (parent_id) REFERENCES law_category(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='法规分类项表';

-- 法规-分类关联（多对多）
CREATE TABLE IF NOT EXISTS law_document_category (
  law_id BIGINT NOT NULL COMMENT '法规ID',
  category_id BIGINT NOT NULL COMMENT '分类ID',
  PRIMARY KEY (law_id, category_id),
  FOREIGN KEY (law_id) REFERENCES law_document(id) ON DELETE CASCADE,
  FOREIGN KEY (category_id) REFERENCES law_category(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='法规分类关联表';

-- 为 law_document 增加分类外键列（兼容现有数据）
ALTER TABLE law_document ADD COLUMN category_id_level BIGINT COMMENT '关联分类-效力层级';
ALTER TABLE law_document ADD COLUMN category_id_dept BIGINT COMMENT '关联分类-法律部门';
ALTER TABLE law_document ADD COLUMN category_id_industry BIGINT COMMENT '关联分类-行业领域';
ALTER TABLE law_document ADD COLUMN category_id_custom BIGINT COMMENT '关联分类-自定义';
ALTER TABLE law_document ADD CONSTRAINT fk_doc_level FOREIGN KEY (category_id_level) REFERENCES law_category(id);
ALTER TABLE law_document ADD CONSTRAINT fk_doc_dept FOREIGN KEY (category_id_dept) REFERENCES law_category(id);
ALTER TABLE law_document ADD CONSTRAINT fk_doc_industry FOREIGN KEY (category_id_industry) REFERENCES law_category(id);
ALTER TABLE law_document ADD CONSTRAINT fk_doc_custom FOREIGN KEY (category_id_custom) REFERENCES law_category(id);

-- 插入初始维度数据
INSERT INTO law_category_type (type_code, type_name, description, sort_order) VALUES
('level', '效力层级', '按法律渊源分类', 1),
('department', '法律部门', '按法律部门分类', 2),
('industry', '行业领域', '按行业领域分类', 3),
('custom', '自定义分类', '用户自定义分类', 4);

-- 插入初始分类数据
INSERT INTO law_category (category_type_id, parent_id, category_code, category_name, color, sort_order) VALUES
-- 效力层级
(1, NULL, 'level_national', '国家法律', '#ef4444', 1),
(1, NULL, 'level_admin', '行政法规', '#f59e0b', 2),
(1, NULL, 'level_local', '地方性法规', '#10b981', 3),
(1, NULL, 'level_judicial', '司法解释', '#3b82f6', 4),
(1, NULL, 'level_other', '其他规范性文件', '#64748b', 5),
-- 法律部门
(2, NULL, 'dept_civil', '民商法', '#8b5cf6', 1),
(2, NULL, 'dept_criminal', '刑法', '#ec4899', 2),
(2, NULL, 'dept_administrative', '行政法', '#14b8a6', 3),
(2, NULL, 'dept_labor', '劳动法', '#f97316', 4),
(2, NULL, 'dept_economic', '经济法', '#84cc16', 5),
(2, NULL, 'dept_constitutional', '宪法', '#06b6d4', 6);
```

### 1.2 创建实体类

**Files:**
- Create: `backend/src/main/java/com/legalai/model/LawCategoryType.java`
- Create: `backend/src/main/java/com/legalai/model/LawCategory.java`
- Create: `backend/src/main/java/com/legalai/model/LawDocumentCategory.java`

**LawCategoryType.java:**
```java
package com.legalai.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("law_category_type")
public class LawCategoryType {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String typeCode;
    private String typeName;
    private String description;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
```

**LawCategory.java:**
```java
package com.legalai.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("law_category")
public class LawCategory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long categoryTypeId;
    private Long parentId;
    private String categoryCode;
    private String categoryName;
    private String color;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
}
```

**LawDocumentCategory.java:**
```java
package com.legalai.model;

import lombok.Data;

@Data
public class LawDocumentCategory {
    private Long lawId;
    private Long categoryId;
}
```

### 1.3 创建 Mapper

**Files:**
- Create: `backend/src/main/java/com/legalai/repository/LawCategoryTypeMapper.java`
- Create: `backend/src/main/java/com/legalai/repository/LawCategoryMapper.java`
- Create: `backend/src/main/java/com/legalai/repository/LawDocumentCategoryMapper.java`

```java
package com.legalai.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.legalai.model.LawCategoryType;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LawCategoryTypeMapper extends BaseMapper<LawCategoryType> {}
```

```java
package com.legalai.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.legalai.model.LawCategory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LawCategoryMapper extends BaseMapper<LawCategory> {}
```

```java
package com.legalai.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.legalai.model.LawDocumentCategory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LawDocumentCategoryMapper extends BaseMapper<LawDocumentCategory> {}
```

---

## Task 2: 分类管理后端接口开发

### 2.1 创建分类 Service

**Files:**
- Create: `backend/src/main/java/com/legalai/admin/service/LawCategoryService.java`

**接口列表：**

| 方法 | 说明 |
|------|------|
| `listTypes()` | 获取所有分类维度 |
| `listCategories(Long typeId)` | 获取某维度下所有分类项（树形） |
| `getCategory(Long id)` | 获取单个分类详情 |
| `createCategory(LawCategory)` | 新增分类项 |
| `updateCategory(Long id, LawCategory)` | 更新分类项 |
| `deleteCategory(Long id)` | 删除分类项（含子分类） |
| `setDocumentCategories(Long lawId, List<Long> categoryIds)` | 设置法规关联分类 |
| `getDocumentCategories(Long lawId)` | 获取法规关联的所有分类 |

### 2.2 在 AdminController 添加分类路由

**Files:**
- Modify: `backend/src/main/java/com/legalai/admin/controller/AdminController.java`

在 `AdminController` 中添加以下路由方法：

```java
// ========== 法规分类管理 ==========
@GetMapping("/law/category-types")
public ApiResponse<List<Map<String, Object>>> listCategoryTypes() {
    return ApiResponse.success(lawCategoryService.listTypes());
}

@GetMapping("/law/categories")
public ApiResponse<List<Map<String, Object>>> listCategories(@RequestParam(required = false) Long typeId) {
    return ApiResponse.success(lawCategoryService.listCategories(typeId));
}

@GetMapping("/law/categories/{id}")
public ApiResponse<Map<String, Object>> getCategory(@PathVariable Long id) {
    return ApiResponse.success(lawCategoryService.getCategory(id));
}

@PostMapping("/law/categories")
public ApiResponse<Void> createCategory(@RequestBody LawCategory category) {
    lawCategoryService.createCategory(category);
    return ApiResponse.success(null);
}

@PutMapping("/law/categories/{id}")
public ApiResponse<Void> updateCategory(@PathVariable Long id, @RequestBody LawCategory category) {
    lawCategoryService.updateCategory(id, category);
    return ApiResponse.success(null);
}

@DeleteMapping("/law/categories/{id}")
public ApiResponse<Void> deleteCategory(@PathVariable Long id) {
    lawCategoryService.deleteCategory(id);
    return ApiResponse.success(null);
}

@GetMapping("/law/document-categories/{lawId}")
public ApiResponse<List<Map<String, Object>>> getDocumentCategories(@PathVariable Long lawId) {
    return ApiResponse.success(lawCategoryService.getDocumentCategories(lawId));
}

@PostMapping("/law/document-categories/{lawId}")
public ApiResponse<Void> setDocumentCategories(@PathVariable Long lawId, @RequestBody Map<String, List<Long>> body) {
    lawCategoryService.setDocumentCategories(lawId, body.get("categoryIds"));
    return ApiResponse.success(null);
}
```

---

## Task 3: 前端分类管理页面

### 3.1 创建 LawCategoryTypes.vue（分类维度管理）

**Files:**
- Create: `frontend/src/views/admin/biz/LawCategoryTypes.vue`

**功能：** 展示所有分类维度（level/department/industry/custom），支持新增/编辑/删除维度。

**布局：**
- 简单表格，左侧类型编码，右侧类型名称 + 描述 + 操作按钮
- 新增/编辑用 Dialog 表单

### 3.2 创建 LawCategories.vue（分类项管理，树形）

**Files:**
- Create: `frontend/src/views/admin/biz/LawCategories.vue`

**功能：** 按维度筛选，展示分类项树形结构（支持多级），CRUD 操作。

**布局：**
- 顶部下拉选择维度
- 左侧树形列表，右侧详情编辑表单
- 支持新增子分类、拖拽排序（可选）

### 3.3 创建 LawImport.vue（AI 导入工作台）

**Files:**
- Create: `frontend/src/views/admin/biz/LawImport.vue`

**功能：** 上传 Word 文件，调用 AI 解析预览，管理员确认后导入。

**布局：**
```
左侧上传区（拖拽 el-upload）
    ↓
右侧预览区（el-card）：
  - 元数据表单（标题/发文字号/发布日期）- 可编辑
  - 分类标签（el-select 多选，从 API 加载分类列表）
  - 章节结构树（el-tree，可编辑条款标题）
    ↓
底部按钮：【取消】【确认导入】
```

**API 调用：**
- `POST /api/v1/admin/law/import/preview` - 上传文件，返回预览数据（不入库）
- `POST /api/v1/admin/law/import/confirm` - 确认导入（入库）
- `GET /api/v1/admin/law-import/history` - 导入历史

### 3.4 注册菜单路由

**Files:**
- Modify: `frontend/src/views/admin/AdminLayout.vue`

在侧边栏菜单中添加：

```js
{
  path: '/admin/law',
  name: 'LawManagement',
  meta: { title: '法规管理' },
  children: [
    { path: '/admin/law/categories', name: 'LawCategories', component: LawCategories, meta: { title: '分类管理' } },
    { path: '/admin/law/import', name: 'LawImport', component: LawImport, meta: { title: 'AI导入' } },
    { path: '/admin/law/list', name: 'Mod07Laws', component: Mod07Laws, meta: { title: '法规列表' } },
  ]
}
```

---

## Task 4: AI 解析流程改造（后端）

### 4.1 创建 AI 解析预览接口

**Files:**
- Create: `backend/src/main/java/com/legalai/dto/LawImportPreview.java`
- Modify: `LawImportService.java`（新增 `previewImport` 方法）
- Modify: `LawImportController.java`（新增 `/preview` 路由）

**LawImportPreview.java:**
```java
package com.legalai.dto;

import lombok.Data;
import java.util.List;

@Data
public class LawImportPreview {
    private String lawTitle;
    private String shortTitle;
    private String documentNo;      // 发文字号
    private String issuingAuthority;
    private String issueDate;
    private String effectiveDate;
    private List<CategorySuggestion> suggestedCategories;
    private List<ChapterNode> chapterTree;
    private List<ArticleParse> articles;

    @Data
    public static class CategorySuggestion {
        private Long categoryTypeId;
        private String typeName;
        private Long categoryId;
        private String categoryName;
        private double confidence;
    }

    @Data
    public static class ChapterNode {
        private String title;
        private int level;
        private List<ChapterNode> children;
    }

    @Data
    public static class ArticleParse {
        private String articleNo;
        private String title;
        private String content;
        private String chapterPath;
    }
}
```

### 4.2 实现 AI 解析逻辑

在 `LawImportService` 中新增 `previewImport(MultipartFile file)` 方法：

```java
public LawImportPreview previewImport(MultipartFile file) {
    // 1. POI 提取纯文本
    String content = extractText(file);

    // 2. AI 提取元数据
    String metaPrompt = "分析以下法律文档，提取：标题、简称、发文字号、发布机关、发布日期、生效日期。以JSON格式返回。\n" + content.substring(0, Math.min(2000, content.length()));
    String metaJson = llmClient.chat(metaPrompt);

    // 3. AI 分类建议
    String categoryPrompt = "判断以下法律文档属于哪些分类，从以下维度回答：\n1. 效力层级（国家法律/行政法规/地方性法规/司法解释/其他）\n2. 法律部门（民商法/刑法/行政法/劳动法/经济法/宪法等）\n3. 行业领域（金融/医疗/教育/互联网/制造业等）\n返回JSON格式，包含分类ID和置信度。\n" + content.substring(0, Math.min(3000, content.length()));
    String categoryJson = llmClient.chat(categoryPrompt);

    // 4. AI 章节结构解析
    String chapterPrompt = "分析以下法律文档的章节结构，返回JSON数组：[{title:章节标题, level:层级(1/2/3), children:[...]}]\n" + content.substring(0, Math.min(5000, content.length()));
    String chapterJson = llmClient.chat(chapterPrompt);

    // 5. 条款提取（按第一章/第一条/等模式切分）
    List<ArticleParse> articles = extractArticles(content);

    return buildPreview(metaJson, categoryJson, chapterJson, articles);
}
```

### 4.3 确认导入接口

在 `LawImportController` 中新增 `/confirm` 路由，接收 `LawImportPreview` 对象，写入 `law_document` + `law_article` + `law_document_category`。

---

## Task 5: 前端 API 层集成

**Files:**
- Modify: `frontend/src/api/index.js`

新增以下 API 方法：

```js
// 法规分类
categoryTypes: () => request.get('/admin/law/category-types'),
categories: (typeId) => request.get('/admin/law/categories', { params: { typeId } }),
createCategory: (data) => request.post('/admin/law/categories', data),
updateCategory: (id, data) => request.put(`/admin/law/categories/${id}`, data),
deleteCategory: (id) => request.delete(`/admin/law/categories/${id}`),
getDocumentCategories: (lawId) => request.get(`/admin/law/document-categories/${lawId}`),
setDocumentCategories: (lawId, categoryIds) => request.post(`/admin/law/document-categories/${lawId}`, { categoryIds }),

// AI 导入
importPreview: (formData) => request.post('/admin/law/import/preview', formData, { headers: { 'Content-Type': 'multipart/form-data' } }),
importConfirm: (data) => request.post('/admin/law/import/confirm', data),
```

---

## Task 6: 集成测试与构建验证

### 验证清单
- [ ] `mvn compile` 编译通过
- [ ] `npm run build` 前端构建通过
- [ ] 数据库迁移脚本执行成功（3 张新表 + 4 个外键列）
- [ ] 分类管理接口返回正确数据
- [ ] 前端分类页面可正常加载和操作
- [ ] Word 文件上传预览接口正常响应
