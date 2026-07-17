package com.legalai.admin.controller;

import com.legalai.admin.service.AdminDataService;
import com.legalai.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/ai")
@CrossOrigin
@Tag(name = "管理后台-AI能力配置", description = "Prompt模板、LLM模型、Milvus向量库、Token用量、KB分块管理")
public class AiConfigController {
    @Autowired
    private AdminDataService adminDataService;

    @Autowired
    private AdminHelper adminHelper;

    @Operation(summary = "查询Prompt模板")
    @GetMapping("/prompts")
    public ApiResponse<Map<String, Object>> listPrompts(
            @RequestParam(required = false) String module,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(adminDataService.list("prompt_template", module, page, pageSize, keyword));
    }

    @GetMapping("/prompts/{id}")
    public ApiResponse<Map<String, Object>> promptDetail(@PathVariable Long id) {
        return ApiResponse.success(adminDataService.detail("prompt_template", id));
    }

    @GetMapping("/prompts/gray-releases")
    public ApiResponse<Map<String, Object>> grayReleases() {
        return ApiResponse.success(adminDataService.list("prompt_gray_release", null, 1, 50, null));
    }

    @GetMapping("/llm-models")
    public ApiResponse<Map<String, Object>> llmModels() {
        return ApiResponse.success(adminDataService.list("llm_model_config", null, 1, 50, null));
    }

    @GetMapping("/llm-models/summary")
    public ApiResponse<Map<String, Object>> llmModelsSummary() {
        return ApiResponse.success(adminDataService.llmModelsSummary());
    }

    @PostMapping("/llm-models/{id}/set-active")
    public ApiResponse<Map<String, Object>> setActiveModel(@PathVariable Long id) {
        boolean ok = adminDataService.setActiveModel(id);
        if (ok) {
            return ApiResponse.success(Map.of("ok", true, "message", "已将当前模型切换到 id=" + id));
        }
        return ApiResponse.error(500, "设置失败");
    }

    @PostMapping("/llm-models/{id}/update-key")
    public ApiResponse<Map<String, Object>> updateModelApiKey(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String apiKey = body.get("apiKey");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return ApiResponse.error(400, "API密钥不能为空");
        }
        boolean ok = adminDataService.updateModelApiKey(id, apiKey);
        if (ok) {
            return ApiResponse.success(Map.of("ok", true, "message", "API密钥已更新"));
        }
        return ApiResponse.error(500, "更新失败");
    }

    @PostMapping("/llm-models")
    public ApiResponse<Map<String, Object>> createModelConfig(@RequestBody Map<String, Object> payload) {
        Map<String, Object> data = adminDataService.createModelConfig(payload);
        adminHelper.recordAudit("CREATE", "llm_model_config", String.valueOf(data.get("id")));
        return ApiResponse.success(data);
    }

    @PutMapping("/llm-models/{id}")
    public ApiResponse<Map<String, Object>> updateModelConfig(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Map<String, Object> data = adminDataService.updateModelConfig(id, payload);
        adminHelper.recordAudit("UPDATE", "llm_model_config", String.valueOf(id));
        return ApiResponse.success(data);
    }

    @DeleteMapping("/llm-models/{id}")
    public ApiResponse<Map<String, Object>> deleteModelConfig(@PathVariable Long id) {
        Map<String, Object> data = adminDataService.deleteModelConfig(id);
        adminHelper.recordAudit("DELETE", "llm_model_config", String.valueOf(id));
        return ApiResponse.success(data);
    }

    @Operation(summary = "Token用量查询")
    @GetMapping("/token-usage")
    public ApiResponse<Map<String, Object>> tokenUsage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(adminDataService.list("llm_token_usage", null, page, pageSize, null));
    }

    @Operation(summary = "KB分块列表")
    @GetMapping("/kb-chunks")
    public ApiResponse<Map<String, Object>> listKbChunks(
            @RequestParam(required = false) Long kbId,
            @RequestParam(required = false) String fileName,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(adminDataService.listKbChunks(kbId, fileName, page, pageSize));
    }

    @GetMapping("/kb-chunks/stats")
    public ApiResponse<Map<String, Object>> kbChunksStats() {
        return ApiResponse.success(adminDataService.kbChunksStats());
    }

    @Operation(summary = "Milvus集合列表")
    @GetMapping("/milvus/collections")
    public ApiResponse<Map<String, Object>> milvusCollections() {
        return ApiResponse.success(adminDataService.milvusCollections());
    }

    @Operation(summary = "LLM健康检查")
    @PostMapping("/llm-models/health-check")
    public ApiResponse<Map<String, Object>> llmHealthCheck() {
        return ApiResponse.success(adminDataService.llmHealthCheck());
    }
}
