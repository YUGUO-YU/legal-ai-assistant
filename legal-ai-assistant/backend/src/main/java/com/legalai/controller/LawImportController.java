package com.legalai.controller;

import com.legalai.dto.ApiResponse;
import com.legalai.dto.LawImportJob;
import com.legalai.dto.LawImportPreview;
import com.legalai.service.LawImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/law-import")
@CrossOrigin
@Tag(name = "法规导入", description = "法规文档导入相关接口")
public class LawImportController {

    private final LawImportService lawImportService;

    public LawImportController(LawImportService lawImportService) {
        this.lawImportService = lawImportService;
    }

    @PostMapping("/web-search")
    @Operation(summary = "网络搜索导入", description = "通过搜索引擎搜索并导入法规")
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
    @Operation(summary = "内容上传导入", description = "直接提交法规内容进行导入")
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
    @Operation(summary = "文件上传导入", description = "上传Word文档进行法规导入")
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
    @Operation(summary = "预设导入", description = "使用预设模板快速导入法规")
    public ApiResponse<LawImportJob> importByPreset(
            @PathVariable String presetKey,
            @RequestParam(value = "operator", defaultValue = "system") String operator) {
        LawImportJob job = lawImportService.importByPreset(presetKey, operator);
        return ApiResponse.success(job);
    }

    @GetMapping("/presets")
    @Operation(summary = "获取预设列表", description = "获取所有可用的预设导入模板")
    public ApiResponse<List<String>> listPresets() {
        return ApiResponse.success(lawImportService.listPresets());
    }

    @GetMapping("/history")
    @Operation(summary = "获取导入历史", description = "分页查询法规导入记录")
    public ApiResponse<Map<String, Object>> listHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        List<LawImportJob> items = lawImportService.listHistory(page, pageSize);
        long total = lawImportService.countHistory();
        return ApiResponse.success(Map.of("items", items, "total", total, "page", page, "pageSize", pageSize));
    }

    @GetMapping("/history/{id}")
    @Operation(summary = "获取导入详情", description = "获取指定导入记录的详细信息")
    public ApiResponse<LawImportJob> getHistory(@PathVariable long id) {
        LawImportJob job = lawImportService.loadJob(id);
        if (job == null) {
            return ApiResponse.error(404, "导入记录不存在");
        }
        return ApiResponse.success(job);
    }

    @GetMapping("/stats")
    @Operation(summary = "导入统计", description = "获取法规导入的统计信息")
    public ApiResponse<Map<String, Long>> stats() {
        return ApiResponse.success(lawImportService.stats());
    }

    @PostMapping("/preview")
    @Operation(summary = "预览导入", description = "预览上传文件中的法规内容")
    public ApiResponse<LawImportPreview> previewImport(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ApiResponse.error(400, "文件不能为空");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".docx") && !filename.endsWith(".doc"))) {
            return ApiResponse.error(400, "只支持 Word 文档(.docx/.doc)");
        }
        LawImportPreview preview = lawImportService.previewImport(file);
        return ApiResponse.success(preview);
    }

    @PostMapping("/confirm")
    @Operation(summary = "确认导入", description = "确认并执行预览中的法规导入")
    public ApiResponse<LawImportJob> confirmImport(@RequestBody LawImportPreview preview) {
        if (preview == null || preview.getLawTitle() == null) {
            return ApiResponse.error(400, "预览数据不能为空");
        }
        LawImportJob job = lawImportService.confirmImport(preview, "admin");
        return ApiResponse.success(job);
    }

    @PostMapping("/direct")
    @Operation(summary = "直接导入", description = "上传文件后一步完成解析与入库，无需预览确认")
    public ApiResponse<LawImportJob> directImport(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "operator", defaultValue = "admin") String operator) {
        if (file == null || file.isEmpty()) {
            return ApiResponse.error(400, "文件不能为空");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".docx") && !filename.endsWith(".doc") && !filename.endsWith(".pdf") && !filename.endsWith(".txt"))) {
            return ApiResponse.error(400, "只支持 Word/PDF/TXT 文档");
        }
        try {
            LawImportJob job = lawImportService.directImport(file, operator);
            return ApiResponse.success(job);
        } catch (Exception e) {
            return ApiResponse.error(500, "直接导入失败: " + e.getMessage());
        }
    }
}
