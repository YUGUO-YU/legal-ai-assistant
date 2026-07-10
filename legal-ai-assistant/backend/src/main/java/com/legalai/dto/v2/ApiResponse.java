package com.legalai.dto.v2;

import lombok.Data;
import java.util.List;

@Data
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private Pagination pagination;
    private Meta meta;
    private ErrorInfo error;

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.data = data;
        response.meta = new Meta();
        return response;
    }

    public static <T> ApiResponse<T> success(T data, Pagination pagination) {
        ApiResponse<T> response = success(data);
        response.pagination = pagination;
        return response;
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.error = new ErrorInfo(code, message);
        response.meta = new Meta();
        return response;
    }

    public static <T> ApiResponse<T> error(int code, String message, String details) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.error = new ErrorInfo(code, message, details);
        response.meta = new Meta();
        return response;
    }

    @Data
    public static class Pagination {
        private int page;
        private int pageSize;
        private long total;
        private int totalPages;
        private boolean hasNext;
        private boolean hasPrev;

        public static Pagination of(int page, int pageSize, long total) {
            Pagination p = new Pagination();
            p.page = page;
            p.pageSize = pageSize;
            p.total = total;
            p.totalPages = (int) Math.ceil((double) total / pageSize);
            p.hasNext = page < p.totalPages;
            p.hasPrev = page > 1;
            return p;
        }
    }

    @Data
    public static class Meta {
        private String requestId;
        private long timestamp;

        public Meta() {
            this.timestamp = System.currentTimeMillis();
        }
    }

    @Data
    public static class ErrorInfo {
        private int code;
        private String message;
        private String details;

        public ErrorInfo(int code, String message) {
            this.code = code;
            this.message = message;
        }

        public ErrorInfo(int code, String message, String details) {
            this.code = code;
            this.message = message;
            this.details = details;
        }
    }
}
