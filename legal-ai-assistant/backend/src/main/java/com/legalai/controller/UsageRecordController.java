package com.legalai.controller;

import com.legalai.dto.ApiResponse;
import com.legalai.dto.UsageRecord;
import com.legalai.service.UsageRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/usage")
@CrossOrigin
@Tag(name = "用量记录", description = "用户操作行为记录相关接口")
public class UsageRecordController {

    @Autowired
    private UsageRecordService usageRecordService;

    @PostMapping("/records")
    @Operation(summary = "添加使用记录", description = "记录用户的操作行为用于分析")
    public ApiResponse<?> addRecord(@RequestBody Map<String, String> request) {
        String userId = request.getOrDefault("userId", "anonymous");
        String type = request.getOrDefault("type", "other");
        String title = request.getOrDefault("title", "");
        String desc = request.getOrDefault("desc", "");

        UsageRecord record = usageRecordService.addRecord(userId, type, title, desc);

        if (record != null) {
            return ApiResponse.success(record);
        } else {
            return ApiResponse.success(Map.of("message", "Redis not available, record not saved"));
        }
    }

    @GetMapping("/records")
    @Operation(summary = "获取使用记录", description = "获取指定用户的操作记录列表")
    public ApiResponse<List<UsageRecord>> getRecords(
            @RequestParam(defaultValue = "anonymous") String userId,
            @RequestParam(defaultValue = "50") int limit) {
        List<UsageRecord> records = usageRecordService.getRecords(userId, limit);
        return ApiResponse.success(records);
    }

    @DeleteMapping("/records/{recordId}")
    @Operation(summary = "删除使用记录", description = "删除指定的使用记录")
    public ApiResponse<Void> deleteRecord(
            @PathVariable String recordId,
            @RequestParam(defaultValue = "anonymous") String userId) {
        usageRecordService.deleteRecord(userId, recordId);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/records")
    @Operation(summary = "清空使用记录", description = "清空指定用户的所有使用记录")
    public ApiResponse<Void> clearAllRecords(@RequestParam(defaultValue = "anonymous") String userId) {
        usageRecordService.clearAllRecords(userId);
        return ApiResponse.success(null);
    }
}
