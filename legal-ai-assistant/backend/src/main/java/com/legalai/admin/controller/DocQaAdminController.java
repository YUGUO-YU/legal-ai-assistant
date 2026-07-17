package com.legalai.admin.controller;

import com.legalai.dto.ApiResponse;
import com.legalai.admin.service.AdminDataService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/biz/mod10/qa-sessions")
@CrossOrigin
public class DocQaAdminController {
    private static final Logger log = LoggerFactory.getLogger(DocQaAdminController.class);

    @Autowired
    private AdminDataService adminDataService;

    @Autowired
    private AdminHelper adminHelper;

    @GetMapping
    public ApiResponse<Map<String, Object>> listSessions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        try {
            Map<String, Object> result = adminDataService.list("doc_qa_session", "MOD-10", page, pageSize, null);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("列出 QA 会话失败: {}", e.getMessage(), e);
            return ApiResponse.error(500, "列出 QA 会话失败: " + e.getMessage());
        }
    }

    @GetMapping("/{sessionId}")
    public ApiResponse<Map<String, Object>> getSessionDetail(@PathVariable Long sessionId) {
        try {
            Map<String, Object> adminUser = adminHelper.getCurrentAdminUser(null);
            Map<String, Object> result = adminDataService.mod10SessionDetail(sessionId);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("获取会话详情失败 sessionId={}: {}", sessionId, e.getMessage(), e);
            return ApiResponse.error(500, "获取会话详情失败: " + e.getMessage());
        }
    }

    @GetMapping("/{sessionId}/messages")
    public ApiResponse<Map<String, Object>> getSessionMessages(
            @PathVariable Long sessionId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize) {
        try {
            Map<String, Object> result = adminDataService.mod10SessionMessages(sessionId, page, pageSize);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("获取会话消息失败 sessionId={}: {}", sessionId, e.getMessage(), e);
            return ApiResponse.error(500, "获取会话消息失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{sessionId}")
    public ApiResponse<Map<String, Object>> deleteSession(@PathVariable Long sessionId, HttpServletRequest request) {
        try {
            Map<String, Object> adminUser = adminHelper.getCurrentAdminUser(request);
            adminHelper.recordAudit("delete", "doc_qa_session", String.valueOf(sessionId));
            Map<String, Object> result = adminDataService.deleteMod10Session(sessionId);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("删除会话失败 sessionId={}: {}", sessionId, e.getMessage(), e);
            return ApiResponse.error(500, "删除会话失败: " + e.getMessage());
        }
    }

    @PostMapping("/{sessionId}/export")
    public ApiResponse<Map<String, Object>> exportSession(@PathVariable Long sessionId) {
        try {
            Map<String, Object> result = adminDataService.exportMod10Session(sessionId);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("导出会话失败 sessionId={}: {}", sessionId, e.getMessage(), e);
            return ApiResponse.error(500, "导出会话失败: " + e.getMessage());
        }
    }
}
