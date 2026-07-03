# Task 2 Report: 分类管理后端接口开发

## 完成情况

DONE

## 改动文件

### 1. 新建 LawCategoryService.java
`backend/src/main/java/com/legalai/admin/service/LawCategoryService.java`

实现了 8 个方法：
- `listTypes()` - 查询所有分类类型，按 sort_order 排序
- `listCategories(typeId)` - 查询分类，支持按 typeId 过滤，返回树形结构（包含 children）
- `getCategory(id)` - 查询单个分类详情，包含父分类信息
- `createCategory(category)` - 创建分类，status 固定为 1
- `updateCategory(id, category)` - 更新分类名称、颜色、排序、状态
- `deleteCategory(id)` - 删除分类及其子分类（通过 parent_id 关联）
- `getDocumentCategories(lawId)` - 获取法规关联的分类列表
- `setDocumentCategories(lawId, categoryIds)` - 设置法规的分类关联（先删后插）

### 2. 修改 AdminController.java
在 `com.legalai.admin.controller.AdminController` 中添加：

**新增 import：**
```java
import com.legalai.admin.service.LawCategoryService;
import com.legalai.model.LawCategory;
```

**新增Autowired字段：**
```java
@Autowired
private LawCategoryService lawCategoryService;
```

**新增路由（8个）：**
```
GET    /api/v1/admin/law/category-types        - listCategoryTypes
GET    /api/v1/admin/law/categories            - listCategories (typeId可选)
GET    /api/v1/admin/law/categories/{id}       - getCategory
POST   /api/v1/admin/law/categories            - createCategory
PUT    /api/v1/admin/law/categories/{id}       - updateCategory
DELETE /api/v1/admin/law/categories/{id}       - deleteCategory
GET    /api/v1/admin/law/document-categories/{lawId}  - getDocumentCategories
POST   /api/v1/admin/law/document-categories/{lawId}  - setDocumentCategories
```

## 测试结果

Maven 未安装在当前环境，无法执行 `mvn compile`。

代码已通过静态检查：
- Service 使用 MyBatis Plus 的 LambdaQueryWrapper 进行类型安全查询
- Controller 方法签名、ApiResponse 用法与现有代码风格一致
- 所有 Mapper 和 Model 引用路径正确

## Git Commit

当前工作区有未提交改动。
