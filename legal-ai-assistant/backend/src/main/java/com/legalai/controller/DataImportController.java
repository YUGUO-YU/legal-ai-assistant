package com.legalai.controller;

import com.legalai.dto.ApiResponse;
import com.legalai.service.DataImportService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/data")
@CrossOrigin
public class DataImportController {

    private final DataImportService dataImportService;

    public DataImportController(DataImportService dataImportService) {
        this.dataImportService = dataImportService;
    }

    @PostMapping("/import-civil-law")
    public ApiResponse<String> importCivilLaw() {
        String result = dataImportService.importCivilLaw();
        return ApiResponse.success(result);
    }

    @PostMapping("/import-labor-law")
    public ApiResponse<String> importLaborLaw() {
        String result = dataImportService.importLaborLaw();
        return ApiResponse.success(result);
    }

    @PostMapping("/import-construction-law")
    public ApiResponse<String> importConstructionLaw() {
        String result = dataImportService.importConstructionLaw();
        return ApiResponse.success(result);
    }

    @PostMapping("/vectorize")
    public ApiResponse<String> vectorizeAll() {
        String result = dataImportService.vectorizeAllArticles();
        return ApiResponse.success(result);
    }

    @PostMapping("/import-all")
    public ApiResponse<String> importAll() {
        String result = dataImportService.importAllData();
        return ApiResponse.success(result);
    }

    @PostMapping("/import-judgments")
    public ApiResponse<String> importJudgments(@RequestBody Map<String, Object> request) {
        String result = dataImportService.importJudgments(request);
        return ApiResponse.success(result);
    }
}