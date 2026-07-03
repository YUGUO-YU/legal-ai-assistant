package com.legalai.service;

import com.legalai.dto.UsageRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UsageRecordService {

    private static final Logger log = LoggerFactory.getLogger(UsageRecordService.class);
    private static final String USAGE_RECORD_PREFIX = "usage:record:";
    private static final String USAGE_RECORD_LIST_KEY = "usage:records:";
    private static final Duration USAGE_RECORD_TTL = Duration.ofHours(24);
    private static final int MAX_RECORDS_PER_USER = 200;

    @Value("${redis.enabled:false}")
    private boolean redisEnabled;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    public UsageRecord addRecord(String userId, String type, String title, String desc) {
        if (!redisEnabled || redisTemplate == null) {
            log.debug("Redis disabled, skipping usage record");
            return null;
        }

        try {
            String recordId = UUID.randomUUID().toString().toLowerCase();
            long timestamp = System.currentTimeMillis();

            UsageRecord record = new UsageRecord(recordId, userId, type, title, desc, timestamp);

            String listKey = USAGE_RECORD_LIST_KEY + userId;

            redisTemplate.opsForList().leftPush(listKey, record);
            redisTemplate.opsForList().trim(listKey, 0, MAX_RECORDS_PER_USER - 1);
            redisTemplate.expire(listKey, USAGE_RECORD_TTL);

            log.debug("Usage record added: userId={}, type={}, title={}", userId, type, title);
            return record;
        } catch (Exception e) {
            log.warn("Failed to add usage record: {}", e.getMessage());
            return null;
        }
    }

    public List<UsageRecord> getRecords(String userId, int limit) {
        List<UsageRecord> records = new ArrayList<>();

        if (!redisEnabled || redisTemplate == null) {
            return records;
        }

        try {
            String listKey = USAGE_RECORD_LIST_KEY + userId;
            List<Object> rawRecords = redisTemplate.opsForList().range(listKey, 0, limit - 1);

            if (rawRecords != null) {
                for (Object obj : rawRecords) {
                    if (obj instanceof UsageRecord) {
                        records.add((UsageRecord) obj);
                    }
                }
            }

            log.debug("Retrieved {} usage records for userId={}", records.size(), userId);
        } catch (Exception e) {
            log.warn("Failed to get usage records: {}", e.getMessage());
        }

        return records;
    }

    public void deleteRecord(String userId, String recordId) {
        if (!redisEnabled || redisTemplate == null) {
            return;
        }

        try {
            String listKey = USAGE_RECORD_LIST_KEY + userId;
            List<Object> rawRecords = redisTemplate.opsForList().range(listKey, 0, -1);

            if (rawRecords != null) {
                for (Object obj : rawRecords) {
                    if (obj instanceof UsageRecord) {
                        UsageRecord record = (UsageRecord) obj;
                        if (record.getId().equals(recordId)) {
                            redisTemplate.opsForList().remove(listKey, 1, record);
                            break;
                        }
                    }
                }
            }

            log.debug("Deleted usage record: userId={}, recordId={}", userId, recordId);
        } catch (Exception e) {
            log.warn("Failed to delete usage record: {}", e.getMessage());
        }
    }

    public void clearAllRecords(String userId) {
        if (!redisEnabled || redisTemplate == null) {
            return;
        }

        try {
            String listKey = USAGE_RECORD_LIST_KEY + userId;
            redisTemplate.delete(listKey);
            log.debug("Cleared all usage records for userId={}", userId);
        } catch (Exception e) {
            log.warn("Failed to clear usage records: {}", e.getMessage());
        }
    }

    public boolean isAvailable() {
        return redisEnabled && redisTemplate != null;
    }
}
