package com.legalai.controller;

import com.legalai.dto.ApiResponse;
import com.legalai.dto.UsageRecord;
import com.legalai.service.UsageRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/usage")
@CrossOrigin
public class UsageRecordController {

    @Autowired
    private UsageRecordService usageRecordService;

    @PostMapping("/records")
    public ApiResponse<UsageRecord> addRecord(@RequestBody Map<String, String> request) {
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
    public ApiResponse<List<UsageRecord>> getRecords(
            @RequestParam(defaultValue = "anonymous") String userId,
            @RequestParam(defaultValue = "50") int limit) {
        List<UsageRecord> records = usageRecordService.getRecords(userId, limit);
        return ApiResponse.success(records);
    }

    @DeleteMapping("/records/{recordId}")
    public ApiResponse<Void> deleteRecord(
            @PathVariable String recordId,
            @RequestParam(defaultValue = "anonymous") String userId) {
        usageRecordService.deleteRecord(userId, recordId);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/records")
    public ApiResponse<Void> clearAllRecords(@RequestParam(defaultValue = "anonymous") String userId) {
        usageRecordService.clearAllRecords(userId);
        return ApiResponse.success(null);
    }
}
