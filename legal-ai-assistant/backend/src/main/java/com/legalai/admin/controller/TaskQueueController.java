package com.legalai.admin.controller;

import com.legalai.dto.ApiResponse;
import com.legalai.service.ProgressNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/tasks")
@CrossOrigin
@Tag(name = "任务队列", description = "后台任务队列管理接口")
public class TaskQueueController {

    @Autowired
    private ProgressNotificationService progressService;

    private final Map<String, TaskInfo> registeredTasks = new HashMap<>();

    @GetMapping("/list")
    @Operation(summary = "获取任务列表", description = "获取所有注册的后台任务及其状态")
    public ApiResponse<List<TaskInfo>> listTasks() {
        return ApiResponse.success(registeredTasks.values().stream()
                .sorted((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()))
                .collect(Collectors.toList()));
    }

    @PostMapping("/register")
    @Operation(summary = "注册任务", description = "注册一个新的后台任务用于进度跟踪")
    public ApiResponse<String> registerTask(
            @Parameter(description = "任务ID") @RequestParam String taskId,
            @Parameter(description = "任务名称") @RequestParam String taskName,
            @Parameter(description = "任务类型") @RequestParam String taskType) {

        TaskInfo info = new TaskInfo();
        info.setTaskId(taskId);
        info.setTaskName(taskName);
        info.setTaskType(taskType);
        info.setStatus("PENDING");
        info.setProgress(0);
        info.setCreatedAt(System.currentTimeMillis());

        registeredTasks.put(taskId, info);
        return ApiResponse.success(taskId);
    }

    @PutMapping("/{taskId}/status")
    @Operation(summary = "更新任务状态", description = "更新指定任务的状态和进度")
    public ApiResponse<Void> updateTaskStatus(
            @PathVariable String taskId,
            @RequestParam String status,
            @RequestParam(defaultValue = "0") int progress,
            @RequestParam(required = false) String message) {

        TaskInfo info = registeredTasks.get(taskId);
        if (info != null) {
            info.setStatus(status);
            info.setProgress(progress);
            info.setMessage(message);
            info.setUpdatedAt(System.currentTimeMillis());
        }

        progressService.notifyProgress(taskId, progress, 100, message, status);

        return ApiResponse.success(null);
    }

    @DeleteMapping("/{taskId}")
    @Operation(summary = "删除任务", description = "从任务列表中移除指定任务")
    public ApiResponse<Void> removeTask(@PathVariable String taskId) {
        registeredTasks.remove(taskId);
        progressService.clearProgress(taskId);
        return ApiResponse.success(null);
    }

    @GetMapping("/{taskId}/progress")
    @Operation(summary = "获取任务进度", description = "获取指定任务的实时进度")
    public ApiResponse<Map<String, Object>> getTaskProgress(@PathVariable String taskId) {
        var progress = progressService.getProgress(taskId);
        if (progress != null) {
            return ApiResponse.success(Map.of(
                    "taskId", taskId,
                    "progress", progress.getPercentage(),
                    "current", progress.getCurrent(),
                    "total", progress.getTotal(),
                    "message", progress.getMessage(),
                    "status", progress.getStatus()
            ));
        }

        TaskInfo info = registeredTasks.get(taskId);
        if (info != null) {
            return ApiResponse.success(Map.of(
                    "taskId", taskId,
                    "progress", info.getProgress(),
                    "message", info.getMessage(),
                    "status", info.getStatus()
            ));
        }

        return ApiResponse.error(404, "任务不存在");
    }

    public static class TaskInfo {
        private String taskId;
        private String taskName;
        private String taskType;
        private String status;
        private int progress;
        private String message;
        private long createdAt;
        private long updatedAt;

        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public String getTaskName() { return taskName; }
        public void setTaskName(String taskName) { this.taskName = taskName; }
        public String getTaskType() { return taskType; }
        public void setTaskType(String taskType) { this.taskType = taskType; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public int getProgress() { return progress; }
        public void setProgress(int progress) { this.progress = progress; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public long getCreatedAt() { return createdAt; }
        public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
        public long getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
    }
}
