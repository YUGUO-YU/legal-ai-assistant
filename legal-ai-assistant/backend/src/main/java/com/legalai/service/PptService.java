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

        List<SlideDTO> slides = generateSlidesFromSearchResults(request);

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

    private List<SlideDTO> generateSlidesFromSearchResults(PptGenerateRequest request) {
        List<SlideDTO> slides = new ArrayList<>();

        SlideDTO coverSlide = new SlideDTO();
        coverSlide.setId(UUID.randomUUID().toString());
        coverSlide.setLayout("title_only");
        coverSlide.setTitle(request.getTitle());
        coverSlide.setBulletPoints(Arrays.asList(
                "基于法律AI助手的智能生成",
                "整理自相关法规和案例检索结果"
        ));
        coverSlide.setNotes("封面幻灯片，介绍PPT主题和来源");
        slides.add(coverSlide);

        if (request.getSearchResults() != null && !request.getSearchResults().isEmpty()) {
            SlideDTO summarySlide = new SlideDTO();
            summarySlide.setId(UUID.randomUUID().toString());
            summarySlide.setLayout("title_content");
            summarySlide.setTitle("检索结果概述");
            List<String> summaryPoints = new ArrayList<>();
            summaryPoints.add("共检索到 " + request.getSearchResults().size() + " 条相关法规");
            summaryPoints.add("涵盖以下法律领域：");
            for (PptGenerateRequest.SearchResultItem item : request.getSearchResults().stream().limit(5).toList()) {
                summaryPoints.add("- " + item.getLawTitle());
            }
            summarySlide.setBulletPoints(summaryPoints);
            summarySlide.setNotes("概述检索到的法律信息");
            slides.add(summarySlide);

            for (int i = 0; i < Math.min(request.getSearchResults().size(), 5); i++) {
                PptGenerateRequest.SearchResultItem item = request.getSearchResults().get(i);
                SlideDTO lawSlide = new SlideDTO();
                lawSlide.setId(UUID.randomUUID().toString());
                lawSlide.setLayout("title_content");
                lawSlide.setTitle(item.getTitle());
                lawSlide.setBulletPoints(Arrays.asList(
                        "法规来源：" + item.getLawTitle(),
                        "条款编号：" + item.getArticleNo(),
                        "内容摘要：" + (item.getContent() != null && item.getContent().length() > 200
                                ? item.getContent().substring(0, 200) + "..."
                                : item.getContent())
                ));
                lawSlide.setNotes("详细展示第" + (i + 1) + "条法规信息");
                slides.add(lawSlide);
            }
        } else {
            SlideDTO summarySlide = new SlideDTO();
            summarySlide.setId(UUID.randomUUID().toString());
            summarySlide.setLayout("title_content");
            summarySlide.setTitle("相关法规参考");
            summarySlide.setBulletPoints(Arrays.asList(
                    "《中华人民共和国民法典》",
                    "《中华人民共和国劳动合同法》",
                    "《中华人民共和国公司法》"
            ));
            slides.add(summarySlide);
        }

        SlideDTO conclusionSlide = new SlideDTO();
        conclusionSlide.setId(UUID.randomUUID().toString());
        conclusionSlide.setLayout("title_content");
        conclusionSlide.setTitle("总结与建议");
        conclusionSlide.setBulletPoints(Arrays.asList(
                "结合本案实际情况，综合考虑相关法律规定",
                "建议咨询专业律师获取个性化法律意见",
                "本PPT内容仅供参考，不构成法律建议"
        ));
        conclusionSlide.setNotes("结语幻灯片，包含法律免责声明");
        slides.add(conclusionSlide);

        return slides;
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
}
