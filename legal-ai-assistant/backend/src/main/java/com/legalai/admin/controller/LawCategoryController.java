package com.legalai.admin.controller;

import com.legalai.admin.service.LawCategoryService;
import com.legalai.dto.ApiResponse;
import com.legalai.model.LawCategory;
import com.legalai.model.LawCategoryType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/law")
@CrossOrigin
@Tag(name = "管理后台-法规分类", description = "法规分类类型、分类及文档分类关联管理")
public class LawCategoryController {
    @Autowired
    private LawCategoryService lawCategoryService;

    @Operation(summary = "查询分类类型列表")
    @GetMapping("/category-types")
    public ApiResponse<List<Map<String, Object>>> listCategoryTypes() {
        return ApiResponse.success(lawCategoryService.listTypes());
    }

    @Operation(summary = "获取分类类型详情")
    @GetMapping("/category-types/{id}")
    public ApiResponse<LawCategoryType> getCategoryType(@PathVariable Long id) {
        return ApiResponse.success(lawCategoryService.getType(id));
    }

    @Operation(summary = "创建分类类型")
    @PostMapping("/category-types")
    public ApiResponse<Void> createCategoryType(@RequestBody LawCategoryType type) {
        lawCategoryService.createType(type);
        return ApiResponse.success(null);
    }

    @Operation(summary = "更新分类类型")
    @PutMapping("/category-types/{id}")
    public ApiResponse<Void> updateCategoryType(@PathVariable Long id, @RequestBody LawCategoryType type) {
        lawCategoryService.updateType(id, type);
        return ApiResponse.success(null);
    }

    @Operation(summary = "删除分类类型")
    @DeleteMapping("/category-types/{id}")
    public ApiResponse<Void> deleteCategoryType(@PathVariable Long id) {
        lawCategoryService.deleteType(id);
        return ApiResponse.success(null);
    }

    @Operation(summary = "查询分类列表")
    @GetMapping("/categories")
    public ApiResponse<List<Map<String, Object>>> listCategories(@RequestParam(required = false) Long typeId) {
        return ApiResponse.success(lawCategoryService.listCategories(typeId));
    }

    @Operation(summary = "获取分类详情")
    @GetMapping("/categories/{id}")
    public ApiResponse<Map<String, Object>> getCategory(@PathVariable Long id) {
        return ApiResponse.success(lawCategoryService.getCategory(id));
    }

    @Operation(summary = "创建分类")
    @PostMapping("/categories")
    public ApiResponse<Void> createCategory(@RequestBody LawCategory category) {
        lawCategoryService.createCategory(category);
        return ApiResponse.success(null);
    }

    @Operation(summary = "更新分类")
    @PutMapping("/categories/{id}")
    public ApiResponse<Void> updateCategory(@PathVariable Long id, @RequestBody LawCategory category) {
        lawCategoryService.updateCategory(id, category);
        return ApiResponse.success(null);
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/categories/{id}")
    public ApiResponse<Void> deleteCategory(@PathVariable Long id) {
        lawCategoryService.deleteCategory(id);
        return ApiResponse.success(null);
    }

    @Operation(summary = "获取文档分类")
    @GetMapping("/document-categories/{lawId}")
    public ApiResponse<List<Map<String, Object>>> getDocumentCategories(@PathVariable Long lawId) {
        return ApiResponse.success(lawCategoryService.getDocumentCategories(lawId));
    }

    @Operation(summary = "设置文档分类")
    @PostMapping("/document-categories/{lawId}")
    public ApiResponse<Void> setDocumentCategories(@PathVariable Long lawId, @RequestBody Map<String, List<Long>> body) {
        lawCategoryService.setDocumentCategories(lawId, body.get("categoryIds"));
        return ApiResponse.success(null);
    }
}
