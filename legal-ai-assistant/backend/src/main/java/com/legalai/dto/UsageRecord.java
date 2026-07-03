package com.legalai.dto;

import java.io.Serializable;
import java.time.Instant;

public class UsageRecord implements Serializable {
    private String id;
    private String userId;
    private String type;
    private String title;
    private String desc;
    private long timestamp;

    public UsageRecord() {
    }

    public UsageRecord(String id, String userId, String type, String title, String desc, long timestamp) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.desc = desc;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDesc() { return desc; }
    public void setDesc(String desc) { this.desc = desc; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
