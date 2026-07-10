package com.legalai.controller;

import com.legalai.dto.ApiResponse;
import com.legalai.service.DataImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/data")
@CrossOrigin
@Tag(name = "数据导入", description = "系统数据批量导入相关接口")
public class DataImportController {

    private final DataImportService dataImportService;

    public DataImportController(DataImportService dataImportService) {
        this.dataImportService = dataImportService;
    }

    @PostMapping("/import-civil-law")
    @Operation(summary = "导入民法典", description = "批量导入民法典及相关法规")
    public ApiResponse<String> importCivilLaw() {
        String result = dataImportService.importCivilLaw();
        return ApiResponse.success(result);
    }

    @PostMapping("/import-labor-law")
    @Operation(summary = "导入劳动法", description = "批量导入劳动法及相关法规")
    public ApiResponse<String> importLaborLaw() {
        String result = dataImportService.importLaborLaw();
        return ApiResponse.success(result);
    }

    @PostMapping("/import-construction-law")
    @Operation(summary = "导入建设工程法", description = "批量导入建设工程法及相关法规")
    public ApiResponse<String> importConstructionLaw() {
        String result = dataImportService.importConstructionLaw();
        return ApiResponse.success(result);
    }

    @PostMapping("/vectorize")
    @Operation(summary = "向量化法规", description = "将所有法规内容向量化存储到向量数据库")
    public ApiResponse<String> vectorizeAll() {
        String result = dataImportService.vectorizeAllArticles();
        return ApiResponse.success(result);
    }

    @PostMapping("/import-all")
    @Operation(summary = "导入所有数据", description = "执行全量数据导入任务")
    public ApiResponse<String> importAll() {
        String result = dataImportService.importAllData();
        return ApiResponse.success(result);
    }

    @PostMapping("/import-judgments")
    @Operation(summary = "导入判例", description = "批量导入司法判例数据")
    public ApiResponse<String> importJudgments(@RequestBody Map<String, Object> request) {
        String result = dataImportService.importJudgments(request);
        return ApiResponse.success(result);
    }
}