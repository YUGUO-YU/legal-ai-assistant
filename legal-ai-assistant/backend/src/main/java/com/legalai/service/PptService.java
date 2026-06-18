package com.legalai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalai.dto.*;
import com.legalai.model.PptDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class PptService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockDataService mockDataService;

    @Autowired
    private PptxGenerator pptxGenerator;

    @Autowired
    private PPTGenerator pptGenerator;

    @Autowired
    private AIService aiService;

    private static final String INSERT_SQL = """
        INSERT INTO ppt_document (ppt_uuid, title, slides_json, template_id, user_id, status, created_at, updated_at)
        VALUES (?, ?, ?, ?, ?, 0, NOW(), NOW())
    """;

    private static final String UPDATE_SQL = """
        UPDATE ppt_document SET title = ?, slides_json = ?, template_id = ?, updated_at = NOW() WHERE id = ?
    """;

    private static final String SELECT_BY_ID = "SELECT * FROM ppt_document WHERE id = ?";

    private static final String SELECT_BY_UUID = "SELECT * FROM ppt_document WHERE ppt_uuid = ?";

    private static final String SELECT_LIST = "SELECT * FROM ppt_document WHERE user_id = ? ORDER BY updated_at DESC";

    private static final String DELETE_SQL = "DELETE FROM ppt_document WHERE id = ?";

    public PptGenerateResponse generatePpt(PptGenerateRequest request) {
        String uuid = "PPT-" + System.currentTimeMillis();
        String userId = request.getUserId() != null ? request.getUserId() : "default";

        List<SlideDTO> slides = pptGenerator.generate(request.getTitle(), request.getSearchResults());

        PptGenerateResponse response = new PptGenerateResponse();
        response.setId(uuid);
        response.setTitle(request.getTitle());
        response.setSlides(slides);
        response.setTemplateId(request.getTemplateId() != null ? request.getTemplateId() : "legal-blue");
        response.setCreatedAt(System.currentTimeMillis());

        try {
            String slidesJson = objectMapper.writeValueAsString(slides);
            jdbcTemplate.update(INSERT_SQL, uuid, request.getTitle(), slidesJson,
                    response.getTemplateId(), userId);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize slides", e);
        }

        return response;
    }

    public PptDocumentDTO getById(Long id) {
        List<PptDocument> results = jdbcTemplate.query(SELECT_BY_ID,
                (rs, rowNum) -> mapRow(rs));
        if (results.isEmpty()) {
            throw new RuntimeException("PPT not found: " + id);
        }
        return toDTO(results.get(0));
    }

    public PptDocumentDTO getByUuid(String uuid) {
        List<PptDocument> results = jdbcTemplate.query(SELECT_BY_UUID,
                (rs, rowNum) -> mapRow(rs), uuid);
        if (results.isEmpty()) {
            throw new RuntimeException("PPT not found: " + uuid);
        }
        return toDTO(results.get(0));
    }

    public PptDocumentDTO update(Long id, PptUpdateRequest request) {
        try {
            String slidesJson = objectMapper.writeValueAsString(request.getSlides());
            jdbcTemplate.update(UPDATE_SQL, request.getTitle(), slidesJson,
                    request.getTemplateId(), id);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize slides", e);
        }
        return getById(id);
    }

    public boolean delete(Long id) {
        int affected = jdbcTemplate.update(DELETE_SQL, id);
        return affected > 0;
    }

    public List<PptDocumentDTO> list(String userId) {
        String sql = userId != null ? SELECT_LIST : "SELECT * FROM ppt_document ORDER BY updated_at DESC";
        List<PptDocument> documents = userId != null
                ? jdbcTemplate.query(sql, (rs, rowNum) -> mapRow(rs), userId)
                : jdbcTemplate.query(sql, (rs, rowNum) -> mapRow(rs));
        return documents.stream().map(this::toDTO).toList();
    }

    public String getFilename(Long id) {
        PptDocumentDTO doc = getById(id);
        String safeTitle = doc.getTitle().replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "_");
        return safeTitle + "_" + doc.getUuid() + ".pptx";
    }

    public byte[] generatePptx(Long id) {
        PptDocumentDTO doc = getById(id);
        return pptxGenerator.generate(doc);
    }

    private PptDocument mapRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        PptDocument doc = new PptDocument();
        doc.setId(rs.getLong("id"));
        doc.setPptUuid(rs.getString("ppt_uuid"));
        doc.setTitle(rs.getString("title"));
        doc.setSlidesJson(rs.getString("slides_json"));
        doc.setTemplateId(rs.getString("template_id"));
        doc.setUserId(rs.getString("user_id"));
        doc.setFilePath(rs.getString("file_path"));
        doc.setFileSize(rs.getLong("file_size"));
        doc.setStatus(rs.getInt("status"));
        doc.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        doc.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return doc;
    }

    private PptDocumentDTO toDTO(PptDocument doc) {
        PptDocumentDTO dto = new PptDocumentDTO();
        dto.setId(String.valueOf(doc.getId()));
        dto.setUuid(doc.getPptUuid());
        dto.setTitle(doc.getTitle());
        dto.setTemplateId(doc.getTemplateId());
        dto.setUserId(doc.getUserId());
        dto.setStatus(doc.getStatus() == 0 ? "editing" : "generated");
        dto.setFileSize(doc.getFileSize());
        dto.setCreatedAt(doc.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        dto.setUpdatedAt(doc.getUpdatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

        try {
            List<SlideDTO> slides = objectMapper.readValue(doc.getSlidesJson(),
                    new TypeReference<List<SlideDTO>>() {});
            dto.setSlides(slides);
        } catch (JsonProcessingException e) {
            dto.setSlides(new ArrayList<>());
        }

        return dto;
    }

    /**
     * AI 增强幻灯片内容：根据标题生成优化后的要点和备注。
     */
    public Map<String, Object> enhanceSlide(Map<String, Object> request) {
        String title = (String) request.getOrDefault("title", "");
        String layout = (String) request.getOrDefault("layout", "title_content");
        @SuppressWarnings("unchecked")
        List<String> currentBullets = (List<String>) request.get("currentBullets");

        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一位法律PPT设计专家。请为以下幻灯片标题生成优化的内容要点。\n\n");
        prompt.append("幻灯片标题：").append(title).append("\n");
        prompt.append("布局类型：").append(layout).append("\n");
        if (currentBullets != null && !currentBullets.isEmpty()) {
            prompt.append("当前要点（供参考）：\n");
            for (String b : currentBullets) {
                prompt.append("- ").append(b).append("\n");
            }
        }
        prompt.append("\n返回JSON格式：\n");
        prompt.append("{\"bulletPoints\":[\"要点1\",\"要点2\",\"要点3\"],\"notes\":\"演讲备注\"}\n");
        prompt.append("\n要求：bulletPoints 每条约15-30字，共3-5条；notes 为一句完整的演讲提示。只返回JSON。");

        Map<String, Object> result = new HashMap<>();
        try {
            String response = aiService.chatWithMessages(List.of(
                Map.of("role", "system", "content", "你是法律PPT内容设计专家，返回简洁专业的JSON。"),
                Map.of("role", "user", "content", prompt.toString())
            ));

            String json = response.trim();
            int start = json.indexOf('{');
            int end = json.lastIndexOf('}');
            if (start >= 0 && end > start) {
                json = json.substring(start, end + 1);
            }
            json = json.replaceAll("```json\\s*", "").replaceAll("```\\s*", "");
            json = json.replaceAll(",(\\s*[}\\]])", "$1");

            @SuppressWarnings("unchecked")
            Map<String, Object> aiResult = objectMapper.readValue(json, Map.class);
            result.put("bulletPoints", aiResult.getOrDefault("bulletPoints",
                currentBullets != null ? currentBullets : List.of(title)));
            result.put("notes", aiResult.getOrDefault("notes", ""));
        } catch (Exception e) {
            if (currentBullets != null) {
                result.put("bulletPoints", currentBullets);
            } else {
                result.put("bulletPoints", List.of(title));
            }
            result.put("notes", "AI增强失败，已保留原内容");
        }
        return result;
    }
}
