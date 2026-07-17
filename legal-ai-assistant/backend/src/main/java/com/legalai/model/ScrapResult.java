package com.legalai.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class ScrapResult {
    private String platform;
    private String rawContent;
    private Map<String, ExtractedField> fields = new HashMap<>();
    private boolean success;
    private String errorCode;
    private long costMs;
    private LocalDateTime timestamp = LocalDateTime.now();

    public ScrapResult() {}

    public ScrapResult(String platform, boolean success) {
        this.platform = platform;
        this.success = success;
    }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public String getRawContent() { return rawContent; }
    public void setRawContent(String rawContent) { this.rawContent = rawContent; }

    public Map<String, ExtractedField> getFields() { return fields; }
    public void setFields(Map<String, ExtractedField> fields) { this.fields = fields; }
    public void addField(String key, ExtractedField field) { this.fields.put(key, field); }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public long getCostMs() { return costMs; }
    public void setCostMs(long costMs) { this.costMs = costMs; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public static class ExtractedField {
        private String fieldName;
        private String value;
        private String rawMatch;
        private double confidence;
        private String source;

        public ExtractedField() {}

        public ExtractedField(String fieldName, String value, double confidence, String source) {
            this.fieldName = fieldName;
            this.value = value;
            this.confidence = confidence;
            this.source = source;
        }

        public String getFieldName() { return fieldName; }
        public void setFieldName(String fieldName) { this.fieldName = fieldName; }

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }

        public String getRawMatch() { return rawMatch; }
        public void setRawMatch(String rawMatch) { this.rawMatch = rawMatch; }

        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }

        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
    }
}
