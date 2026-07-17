package com.legalai.admin.controller;

import com.legalai.admin.enums.DataScope;
import com.legalai.admin.service.AdminDataService;
import com.legalai.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/ops")
@CrossOrigin
@Tag(name = "管理后台-运营分析", description = "用户反馈、搜索日志、导出功能")
public class OpsController {
    @Autowired
    private AdminDataService adminDataService;

    @Autowired
    private AdminHelper adminHelper;

    @Operation(summary = "查询用户反馈")
    @GetMapping("/user-feedback")
    public ApiResponse<Map<String, Object>> userFeedback(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request) {
        Map<String, Object> adminInfo = adminHelper.getCurrentAdminUser(request);
        Long adminUserId = (Long) adminInfo.get("userId");
        DataScope dataScope = (DataScope) adminInfo.get("dataScope");
        return ApiResponse.success(adminDataService.listUserFeedback(adminUserId, dataScope, page, pageSize));
    }

    @PostMapping("/user-feedback/{id}/update")
    public ApiResponse<Map<String, Object>> updateUserFeedback(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        adminHelper.recordAudit("UPDATE", "user_feedback", String.valueOf(id));
        return ApiResponse.success(adminDataService.updateUserFeedback(id, data));
    }

    @Operation(summary = "查询搜索日志")
    @GetMapping("/search-logs")
    public ApiResponse<Map<String, Object>> searchLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize,
            HttpServletRequest request) {
        Map<String, Object> adminInfo = adminHelper.getCurrentAdminUser(request);
        Long adminUserId = (Long) adminInfo.get("userId");
        DataScope dataScope = (DataScope) adminInfo.get("dataScope");
        return ApiResponse.success(adminDataService.listSearchLogs(adminUserId, dataScope, page, pageSize));
    }

    @GetMapping(value = "/search-logs/export", produces = "text/csv")
    public String exportSearchLogs() {
        adminHelper.recordAudit("EXPORT", "search_log", null);
        return adminDataService.exportSearchLogsCsv();
    }

    @GetMapping(value = "/user-feedback/export", produces = "text/csv")
    public String exportUserFeedback() {
        adminHelper.recordAudit("EXPORT", "user_feedback", null);
        return adminDataService.exportUserFeedbackCsv();
    }
}
