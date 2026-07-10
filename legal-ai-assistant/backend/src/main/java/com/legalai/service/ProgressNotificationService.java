package com.legalai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Service
public class ProgressNotificationService {

    private static final Logger log = LoggerFactory.getLogger(ProgressNotificationService.class);

    private final Map<String, CopyOnWriteArrayList<ProgressListener>> listeners = new ConcurrentHashMap<>();
    private final Map<String, ProgressInfo> progressCache = new ConcurrentHashMap<>();

    public String subscribe(String taskId, Consumer<ProgressInfo> onProgress) {
        ProgressListener listener = new ProgressListener(taskId, onProgress);
        listeners.computeIfAbsent(taskId, k -> new CopyOnWriteArrayList<>()).add(listener);

        ProgressInfo cached = progressCache.get(taskId);
        if (cached != null) {
            onProgress.accept(cached);
        }

        log.debug("进度订阅: taskId={}, 当前订阅数={}", taskId, getListenerCount(taskId));
        return listener.getId();
    }

    public void unsubscribe(String taskId, String listenerId) {
        CopyOnWriteArrayList<ProgressListener> taskListeners = listeners.get(taskId);
        if (taskListeners != null) {
            taskListeners.removeIf(l -> l.getId().equals(listenerId));
            log.debug("进度取消订阅: taskId={}, listenerId={}", taskId, listenerId);
        }
    }

    public void notifyProgress(String taskId, int current, int total, String message, String status) {
        ProgressInfo info = new ProgressInfo();
        info.setTaskId(taskId);
        info.setCurrent(current);
        info.setTotal(total);
        info.setPercentage(total > 0 ? (current * 100) / total : 0);
        info.setMessage(message);
        info.setStatus(status);
        info.setTimestamp(System.currentTimeMillis());

        progressCache.put(taskId, info);

        CopyOnWriteArrayList<ProgressListener> taskListeners = listeners.get(taskId);
        if (taskListeners != null && !taskListeners.isEmpty()) {
            for (ProgressListener listener : taskListeners) {
                try {
                    listener.onProgress(info);
                } catch (Exception e) {
                    log.warn("进度通知失败: taskId={}, error={}", taskId, e.getMessage());
                }
            }
        }

        log.debug("进度更新: taskId={}, {}/{} ({}%), status={}", taskId, current, total, info.getPercentage(), status);
    }

    public void notifySuccess(String taskId, String message) {
        notifyProgress(taskId, 100, 100, message, "COMPLETED");
        cleanup(taskId);
    }

    public void notifyError(String taskId, String message) {
        notifyProgress(taskId, 0, 100, message, "FAILED");
        cleanup(taskId);
    }

    public ProgressInfo getProgress(String taskId) {
        return progressCache.get(taskId);
    }

    public void clearProgress(String taskId) {
        progressCache.remove(taskId);
        listeners.remove(taskId);
    }

    private void cleanup(String taskId) {
        CopyOnWriteArrayList<ProgressListener> taskListeners = listeners.get(taskId);
        if (taskListeners != null) {
            taskListeners.clear();
        }
        listeners.remove(taskId);
    }

    private int getListenerCount(String taskId) {
        CopyOnWriteArrayList<ProgressListener> taskListeners = listeners.get(taskId);
        return taskListeners != null ? taskListeners.size() : 0;
    }

    public static class ProgressInfo {
        private String taskId;
        private int current;
        private int total;
        private int percentage;
        private String message;
        private String status;
        private long timestamp;

        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public int getCurrent() { return current; }
        public void setCurrent(int current) { this.current = current; }
        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }
        public int getPercentage() { return percentage; }
        public void setPercentage(int percentage) { this.percentage = percentage; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }

    private static class ProgressListener {
        private final String id;
        private final String taskId;
        private final Consumer<ProgressInfo> onProgress;

        ProgressListener(String taskId, Consumer<ProgressInfo> onProgress) {
            this.id = java.util.UUID.randomUUID().toString();
            this.taskId = taskId;
            this.onProgress = onProgress;
        }

        String getId() { return id; }

        void onProgress(ProgressInfo info) {
            onProgress.accept(info);
        }
    }
}
