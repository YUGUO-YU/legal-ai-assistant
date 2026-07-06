package com.legalai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalai.dto.ContractReviewResponse;
import com.legalai.util.IdGenerator;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ContractReviewStore {
    private static final Logger log = LoggerFactory.getLogger(ContractReviewStore.class);

    private final Map<String, ContractReviewResponse> cache = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private JdbcTemplate jdbc;

    @PostConstruct
    public void init() {
        loadFromDatabase();
    }

    private void loadFromDatabase() {
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT review_uuid, risk_level, risk_count, summary, risk_details, created_at, " +
                "file_name, file_size, review_type, user_id, username " +
                "FROM contract_review ORDER BY created_at DESC LIMIT 1000"
            );
            for (Map<String, Object> row : rows) {
                ContractReviewResponse resp = mapRowToResponse(row);
                if (resp != null && resp.getReviewUuid() != null) {
                    cache.put(resp.getReviewUuid(), resp);
                }
            }
            log.info("[ContractReviewStore] 从数据库加载 {} 条审查记录", cache.size());
        } catch (Exception e) {
            log.warn("[ContractReviewStore] 从数据库加载失败，将使用空缓存: {}", e.getMessage());
        }
    }

    public String save(ContractReviewResponse response) {
        if (response == null) {
            return null;
        }
        String uuid = IdGenerator.generateUUID();
        response.setReviewUuid(uuid);
        cache.put(uuid, response);
        saveToDatabase(response);
        return uuid;
    }

    private void saveToDatabase(ContractReviewResponse response) {
        try {
            String riskDetails = null;
            if (response.getHighRiskItems() != null || response.getMediumRiskItems() != null || response.getLowRiskItems() != null) {
                riskDetails = objectMapper.writeValueAsString(response);
            }
            String sql = """
                INSERT INTO contract_review (review_uuid, user_id, username, file_name, file_size, review_type, risk_level, risk_count, summary, risk_details, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
            jdbc.update(sql,
                response.getReviewUuid(),
                null,
                null,
                response.getContractName(),
                null,
                null,
                response.getRiskLevel() != null ? response.getRiskLevel().toLowerCase() : null,
                countRisks(response),
                response.getOverallComment(),
                riskDetails,
                response.getCreatedAt() != null ? new java.sql.Timestamp(response.getCreatedAt()) : new java.sql.Timestamp(System.currentTimeMillis())
            );
        } catch (Exception e) {
            log.warn("[ContractReviewStore] 保存审查记录到数据库失败: {}", e.getMessage());
        }
    }

    private int countRisks(ContractReviewResponse response) {
        int count = 0;
        if (response.getHighRiskItems() != null) count += response.getHighRiskItems().size();
        if (response.getMediumRiskItems() != null) count += response.getMediumRiskItems().size();
        if (response.getLowRiskItems() != null) count += response.getLowRiskItems().size();
        return count;
    }

    public ContractReviewResponse get(String uuid) {
        if (uuid == null) return null;
        ContractReviewResponse cached = cache.get(uuid);
        if (cached != null) return cached;
        return getFromDatabase(uuid);
    }

    private ContractReviewResponse getFromDatabase(String uuid) {
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT review_uuid, risk_level, risk_count, summary, risk_details, created_at, " +
                "file_name, file_size, review_type, user_id, username " +
                "FROM contract_review WHERE review_uuid = ?", uuid
            );
            if (!rows.isEmpty()) {
                ContractReviewResponse resp = mapRowToResponse(rows.get(0));
                if (resp != null) {
                    cache.put(uuid, resp);
                }
                return resp;
            }
        } catch (Exception e) {
            log.warn("[ContractReviewStore] 从数据库查询审查记录失败: {}", e.getMessage());
        }
        return null;
    }

    public List<ContractReviewResponse> listRecent(int limit) {
        List<ContractReviewResponse> values = new ArrayList<>(cache.values());
        values.sort(Comparator.comparing(
            r -> r.getReviewUuid() == null ? "" : r.getReviewUuid(),
            Comparator.reverseOrder()
        ));
        int size = Math.min(limit, values.size());
        return new ArrayList<>(values.subList(0, size));
    }

    public void remove(String uuid) {
        if (uuid != null) {
            cache.remove(uuid);
            try {
                jdbc.update("DELETE FROM contract_review WHERE review_uuid = ?", uuid);
            } catch (Exception e) {
                log.warn("[ContractReviewStore] 从数据库删除审查记录失败: {}", e.getMessage());
            }
        }
    }

    private ContractReviewResponse mapRowToResponse(Map<String, Object> row) {
        try {
            String riskDetails = row.get("risk_details") != null ? row.get("risk_details").toString() : null;
            if (riskDetails != null && riskDetails.startsWith("{")) {
                ContractReviewResponse resp = objectMapper.readValue(riskDetails, ContractReviewResponse.class);
                if (row.get("created_at") != null) {
                    resp.setCreatedAt(((java.sql.Timestamp) row.get("created_at")).getTime());
                }
                if (row.get("file_name") != null) {
                    resp.setContractName(row.get("file_name").toString());
                }
                return resp;
            }
            ContractReviewResponse resp = new ContractReviewResponse();
            resp.setReviewUuid(row.get("review_uuid") != null ? row.get("review_uuid").toString() : null);
            resp.setRiskLevel(row.get("risk_level") != null ? row.get("risk_level").toString() : null);
            resp.setOverallComment(row.get("summary") != null ? row.get("summary").toString() : null);
            if (row.get("created_at") != null) {
                resp.setCreatedAt(((java.sql.Timestamp) row.get("created_at")).getTime());
            }
            if (row.get("file_name") != null) {
                resp.setContractName(row.get("file_name").toString());
            }
            return resp;
        } catch (Exception e) {
            log.warn("[ContractReviewStore] 解析审查记录失败: {}", e.getMessage());
            return null;
        }
    }
}
