package com.legalai.controller;

import com.legalai.dto.ApiResponse;
import com.legalai.dto.LawImportJob;
import com.legalai.service.LawImportService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/law-import")
@CrossOrigin
public class LawImportController {

    private final LawImportService lawImportService;

    public LawImportController(LawImportService lawImportService) {
        this.lawImportService = lawImportService;
    }

    @PostMapping("/web-search")
    public ApiResponse<LawImportJob> importByWebSearch(@RequestBody Map<String, String> request) {
        String lawName = request.get("lawName");
        String operator = request.getOrDefault("operator", "system");
        if (lawName == null || lawName.isBlank()) {
            return ApiResponse.error(400, "lawName 不能为空");
        }
        LawImportJob job = lawImportService.importByWebSearch(lawName, operator);
        return ApiResponse.success(job);
    }

    @PostMapping("/upload")
    public ApiResponse<LawImportJob> importByUpload(@RequestBody Map<String, Object> request) {
        String lawName = (String) request.get("lawName");
        String content = (String) request.get("content");
        Object operatorObj = request.get("operator");
        String operator = operatorObj != null ? operatorObj.toString() : "system";
        if (lawName == null || lawName.isBlank()) {
            return ApiResponse.error(400, "lawName 不能为空");
        }
        if (content == null || content.isBlank()) {
            return ApiResponse.error(400, "content 不能为空");
        }
        LawImportJob job = lawImportService.importByUpload(lawName, content, operator);
        return ApiResponse.success(job);
    }

    @PostMapping("/upload-file")
    public ApiResponse<LawImportJob> importByUploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "lawName", required = false) String lawName,
            @RequestParam(value = "operator", defaultValue = "system") String operator) {
        if (file == null || file.isEmpty()) {
            return ApiResponse.error(400, "文件不能为空");
        }
        try {
            String content = new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
            String name = (lawName != null && !lawName.isBlank()) ? lawName : file.getOriginalFilename();
            if (name != null && name.endsWith(".json")) {
                name = name.substring(0, name.length() - 5);
            }
            LawImportJob job = lawImportService.importByUpload(name, content, operator);
            return ApiResponse.success(job);
        } catch (Exception e) {
            return ApiResponse.error(500, "读取文件失败: " + e.getMessage());
        }
    }

    @PostMapping("/preset/{presetKey}")
    public ApiResponse<LawImportJob> importByPreset(
            @PathVariable String presetKey,
            @RequestParam(value = "operator", defaultValue = "system") String operator) {
        LawImportJob job = lawImportService.importByPreset(presetKey, operator);
        return ApiResponse.success(job);
    }

    @GetMapping("/presets")
    public ApiResponse<List<String>> listPresets() {
        return ApiResponse.success(lawImportService.listPresets());
    }

    @GetMapping("/history")
    public ApiResponse<Map<String, Object>> listHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        List<LawImportJob> items = lawImportService.listHistory(page, pageSize);
        long total = lawImportService.countHistory();
        return ApiResponse.success(Map.of("items", items, "total", total, "page", page, "pageSize", pageSize));
    }

    @GetMapping("/history/{id}")
    public ApiResponse<LawImportJob> getHistory(@PathVariable long id) {
        LawImportJob job = lawImportService.loadJob(id);
        if (job == null) {
            return ApiResponse.error(404, "导入记录不存在");
        }
        return ApiResponse.success(job);
    }

    @GetMapping("/stats")
    public ApiResponse<Map<String, Long>> stats() {
        return ApiResponse.success(lawImportService.stats());
    }
}
