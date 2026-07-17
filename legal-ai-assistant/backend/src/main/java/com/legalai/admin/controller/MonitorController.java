package com.legalai.admin.controller;

import com.legalai.admin.service.AdminDataService;
import com.legalai.admin.service.AlertMonitorService;
import com.legalai.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/monitor")
@CrossOrigin
@Tag(name = "管理后台-监控告警", description = "告警规则、历史、监控概览")
public class MonitorController {
    @Autowired
    private AdminDataService adminDataService;

    @Autowired
    private AdminHelper adminHelper;

    @Autowired
    private AlertMonitorService alertMonitorService;

    @Operation(summary = "查询告警规则")
    @GetMapping("/alert-rules")
    public ApiResponse<Map<String, Object>> alertRules() {
        return ApiResponse.success(adminDataService.list("alert_rule", null, 1, 100, null));
    }

    @PostMapping("/alert-rules")
    public ApiResponse<Map<String, Object>> createAlertRule(@RequestBody Map<String, Object> data) {
        adminHelper.recordAudit("CREATE", "alert_rule", null);
        return ApiResponse.success(adminDataService.createAlertRule(data));
    }

    @PutMapping("/alert-rules/{id}")
    public ApiResponse<Map<String, Object>> updateAlertRule(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        adminHelper.recordAudit("UPDATE", "alert_rule", String.valueOf(id));
        return ApiResponse.success(adminDataService.updateAlertRule(id, data));
    }

    @DeleteMapping("/alert-rules/{id}")
    public ApiResponse<Map<String, Object>> deleteAlertRule(@PathVariable Long id) {
        adminHelper.recordAudit("DELETE", "alert_rule", String.valueOf(id));
        return ApiResponse.success(adminDataService.deleteAlertRule(id));
    }

    @PostMapping("/alert-rules/{id}/toggle")
    public ApiResponse<Map<String, Object>> toggleAlertRule(@PathVariable Long id) {
        adminHelper.recordAudit("TOGGLE", "alert_rule", String.valueOf(id));
        return ApiResponse.success(adminDataService.toggleAlertRule(id));
    }

    @Operation(summary = "查询告警历史")
    @GetMapping("/alert-history")
    public ApiResponse<Map<String, Object>> alertHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(adminDataService.list("alert_history", null, page, pageSize, null));
    }

    @GetMapping("/alert_history/list")
    public ApiResponse<Map<String, Object>> alertHistoryList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(adminDataService.list("alert_history", null, page, pageSize, null));
    }

    @PostMapping("/alert-check")
    public ApiResponse<Map<String, Object>> triggerAlertCheck() {
        return ApiResponse.success(alertMonitorService.checkAlerts());
    }

    @Operation(summary = "监控概览")
    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> monitorOverview() {
        return ApiResponse.success(adminDataService.monitorOverview());
    }

    @GetMapping(value = "/alert-rules/export", produces = "text/csv")
    public String exportAlertRules() {
        adminHelper.recordAudit("EXPORT", "alert_rule", null);
        return adminDataService.exportAlertRulesCsv();
    }

    @GetMapping(value = "/alert-history/export", produces = "text/csv")
    public String exportAlertHistory() {
        adminHelper.recordAudit("EXPORT", "alert_history", null);
        return adminDataService.exportAlertHistoryCsv();
    }
}
