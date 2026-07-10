package com.legalai.controller;

import com.legalai.service.ProgressNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/progress")
@CrossOrigin
@Tag(name = "进度通知", description = "长时间任务进度实时推送接口")
public class ProgressController {

    private static final long SSE_TIMEOUT = 30 * 60 * 1000L;

    @Autowired
    private ProgressNotificationService progressService;

    private final Map<String, SseEmitter> activeEmitters = new ConcurrentHashMap<>();

    @GetMapping(value = "/subscribe/{taskId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "订阅任务进度", description = "通过SSE实时接收任务进度更新")
    public SseEmitter subscribeProgress(
            @Parameter(description = "任务ID") @PathVariable String taskId) {

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        String subscriptionId = progressService.subscribe(taskId, info -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("progress")
                        .data(info));
            } catch (Exception e) {
                emitter.complete();
            }
        });

        emitter.onCompletion(() -> {
            progressService.unsubscribe(taskId, subscriptionId);
            activeEmitters.remove(taskId);
        });

        emitter.onTimeout(() -> {
            progressService.unsubscribe(taskId, subscriptionId);
            activeEmitters.remove(taskId);
        });

        emitter.onError(e -> {
            progressService.unsubscribe(taskId, subscriptionId);
            activeEmitters.remove(taskId);
        });

        activeEmitters.put(taskId, emitter);

        try {
            var currentProgress = progressService.getProgress(taskId);
            if (currentProgress != null) {
                emitter.send(SseEmitter.event()
                        .name("progress")
                        .data(currentProgress));
            }
        } catch (Exception e) {
            // Ignore initial send errors
        }

        return emitter;
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "获取任务进度", description = "查询当前任务的进度状态")
    public Map<String, Object> getProgress(@PathVariable String taskId) {
        var progress = progressService.getProgress(taskId);
        if (progress == null) {
            return Map.of(
                "taskId", taskId,
                "status", "NOT_FOUND",
                "message", "任务不存在或已过期"
            );
        }
        return Map.of(
            "taskId", progress.getTaskId(),
            "current", progress.getCurrent(),
            "total", progress.getTotal(),
            "percentage", progress.getPercentage(),
            "message", progress.getMessage(),
            "status", progress.getStatus()
        );
    }

    @DeleteMapping("/{taskId}")
    @Operation(summary = "清除任务进度", description = "清除任务进度缓存")
    public Map<String, Object> clearProgress(@PathVariable String taskId) {
        progressService.clearProgress(taskId);
        SseEmitter emitter = activeEmitters.remove(taskId);
        if (emitter != null) {
            emitter.complete();
        }
        return Map.of("success", true, "taskId", taskId);
    }
}
