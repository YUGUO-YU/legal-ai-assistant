package com.legalai.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.legalai.llm.LLMClient;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class JudgmentImportService {
    private static final Logger log = LoggerFactory.getLogger(JudgmentImportService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired(required = false)
    private LLMClient llmClient;

    public Map<String, Object> previewImport(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        String content;
        if (filename.endsWith(".docx")) {
            content = extractTextFromDocx(file);
        } else if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
            content = extractTextFromExcel(file);
        } else {
            throw new IllegalArgumentException("不支持的文件格式，仅支持 .docx 和 .xlsx");
        }

        Map<String, Object> parsed = parseJudgmentContent(content);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cases = (List<Map<String, Object>>) parsed.get("cases");

        List<Map<String, Object>> previewRows = new ArrayList<>();
        int errors = 0;
        for (int i = 0; i < Math.min(cases.size(), 10); i++) {
            Map<String, Object> c = cases.get(i);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("caseName", c.getOrDefault("caseName", ""));
            row.put("caseNo", c.getOrDefault("caseNo", ""));
            row.put("courtName", c.getOrDefault("courtName", ""));
            row.put("judgeDate", c.getOrDefault("judgeDate", ""));
            row.put("caseCause", c.getOrDefault("caseCause", ""));
            row.put("judgmentResult", c.getOrDefault("judgmentResult", ""));
            boolean valid = !String.valueOf(c.getOrDefault("caseName", "")).isEmpty();
            row.put("valid", valid);
            if (!valid) {
                errors++;
                row.put("error", "案件名称不能为空");
            }
            previewRows.add(row);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("previewRows", previewRows);
        result.put("totalRows", cases.size());
        result.put("errors", errors);
        result.put("data", cases);
        return result;
    }

    public Map<String, Object> confirmImport(List<Map<String, Object>> cases) {
        return confirmImport(cases, null);
    }

    public Map<String, Object> confirmImport(List<Map<String, Object>> cases, java.util.function.Consumer<Integer> progressCallback) {
        int imported = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();
        int total = cases.size();

        String insertSql = """
            INSERT INTO tb_case
                (case_uuid, case_no, case_name, case_type, case_cause, court_level, court_name,
                 judge_date, trial_procedure, judgment_result, litigation_amount, plaintiff, defendant,
                 key_facts, judgment_summary, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
            ON DUPLICATE KEY UPDATE
                case_no = VALUES(case_no), case_name = VALUES(case_name), case_type = VALUES(case_type),
                case_cause = VALUES(case_cause), court_level = VALUES(court_level), court_name = VALUES(court_name),
                judge_date = VALUES(judge_date), trial_procedure = VALUES(trial_procedure),
                judgment_result = VALUES(judgment_result), litigation_amount = VALUES(litigation_amount),
                plaintiff = VALUES(plaintiff), defendant = VALUES(defendant),
                key_facts = VALUES(key_facts), judgment_summary = VALUES(judgment_summary)
        """;

        for (int i = 0; i < cases.size(); i++) {
            Map<String, Object> c = cases.get(i);
            try {
                String caseName = String.valueOf(c.getOrDefault("caseName", ""));
                if (caseName.isEmpty() || "null".equals(caseName)) {
                    skipped++;
                    if (progressCallback != null) progressCallback.accept(i + 1);
                    continue;
                }

                String caseUuid = "CASE-" + System.currentTimeMillis() + "-" + Math.abs(caseName.hashCode() % 10000);
                String caseNo = String.valueOf(c.getOrDefault("caseNo", ""));
                if ("null".equals(caseNo)) caseNo = "";

                Integer caseType = parseCaseType(String.valueOf(c.getOrDefault("caseType", "")));
                String caseCause = String.valueOf(c.getOrDefault("caseCause", ""));
                if ("null".equals(caseCause)) caseCause = "";

                Integer courtLevel = parseCourtLevel(String.valueOf(c.getOrDefault("courtLevel", "")));
                String courtName = String.valueOf(c.getOrDefault("courtName", ""));
                if ("null".equals(courtName)) courtName = "";

                java.sql.Date judgeDate = parseDate(String.valueOf(c.getOrDefault("judgeDate", "")));
                String trialProcedure = String.valueOf(c.getOrDefault("trialProcedure", ""));
                if ("null".equals(trialProcedure)) trialProcedure = "";

                Integer judgmentResult = parseJudgmentResult(String.valueOf(c.getOrDefault("judgmentResult", "")));
                BigDecimal litigationAmount = parseAmount(String.valueOf(c.getOrDefault("litigationAmount", "0")));

                String plaintiff = String.valueOf(c.getOrDefault("plaintiff", ""));
                if ("null".equals(plaintiff)) plaintiff = "";

                String defendant = String.valueOf(c.getOrDefault("defendant", ""));
                if ("null".equals(defendant)) defendant = "";

                String keyFacts = String.valueOf(c.getOrDefault("keyFacts", ""));
                if ("null".equals(keyFacts)) keyFacts = "";

                String judgmentSummary = String.valueOf(c.getOrDefault("judgmentSummary", ""));
                if ("null".equals(judgmentSummary)) judgmentSummary = "";

                jdbcTemplate.update(insertSql,
                        caseUuid, caseNo, caseName, caseType, caseCause, courtLevel, courtName,
                        judgeDate, trialProcedure, judgmentResult, litigationAmount,
                        plaintiff, defendant, keyFacts, judgmentSummary);

                imported++;
            } catch (Exception e) {
                log.warn("导入案例失败: {}, error: {}", c, e.getMessage());
                errors.add("案例 " + c.getOrDefault("caseName", "未知") + " 导入失败: " + e.getMessage());
                skipped++;
            }
            if (progressCallback != null) progressCallback.accept(i + 1);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("imported", imported);
        result.put("skipped", skipped);
        result.put("errors", errors);
        return result;
    }

    private String extractTextFromDocx(MultipartFile file) {
        try (XWPFDocument doc = new XWPFDocument(file.getInputStream())) {
            XWPFWordExtractor extractor = new XWPFWordExtractor(doc);
            return extractor.getText();
        } catch (Exception e) {
            throw new RuntimeException("Word文档解析失败: " + e.getMessage(), e);
        }
    }

    private String extractTextFromExcel(MultipartFile file) {
        StringBuilder sb = new StringBuilder();
        try (InputStream is = file.getInputStream();
              Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                StringBuilder rowText = new StringBuilder();
                for (Cell cell : row) {
                    if (cell != null) {
                        rowText.append(getCellValue(cell)).append("\t");
                    }
                }
                if (rowText.length() > 0) {
                    sb.append(rowText).append("\n");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Excel解析失败: " + e.getMessage(), e);
        }
        return sb.toString();
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private Map<String, Object> parseJudgmentContent(String content) {
        List<Map<String, Object>> cases = new ArrayList<>();

        if (llmClient != null) {
            try {
                String prompt = """
                    从以下裁判文书内容中提取案例信息，返回JSON数组格式：
                    [{
                        "caseName": "案件名称",
                        "caseNo": "案号",
                        "courtName": "法院名称",
                        "judgeDate": "裁判日期(YYYY-MM-DD)",
                        "caseCause": "案由",
                        "caseType": "案件类型(1民事 2刑事 3行政)",
                        "courtLevel": "法院层级(1最高院 2高院 3中院 4基层院)",
                        "judgmentResult": "裁判结果(1全部支持 2部分支持 3驳回)",
                        "litigationAmount": "诉讼金额",
                        "plaintiff": "原告",
                        "defendant": "被告",
                        "keyFacts": "关键事实",
                        "judgmentSummary": "裁判摘要"
                    }]
                    内容：
                    """ + content.substring(0, Math.min(8000, content.length()));

                String jsonResponse = llmClient.chat(prompt);
                cases = parseJsonArray(jsonResponse);
                if (!cases.isEmpty()) {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("cases", cases);
                    return result;
                }
            } catch (Exception e) {
                log.warn("LLM解析失败，使用简单解析: {}", e.getMessage());
            }
        }

        cases = simpleParse(content);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cases", cases);
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseJsonArray(String json) {
        List<Map<String, Object>> cases = new ArrayList<>();
        try {
            json = json.trim();
            int start = json.indexOf('[');
            int end = json.lastIndexOf(']');
            if (start >= 0 && end > start) {
                json = json.substring(start, end + 1);
            }
            json = json.replaceAll("```json\\s*", "").replaceAll("```\\s*", "");

            ObjectMapper mapper = new ObjectMapper();
            var list = mapper.readValue(json, List.class);
            for (var item : list) {
                if (item instanceof Map) {
                    cases.add((Map<String, Object>) item);
                }
            }
        } catch (Exception e) {
            log.warn("JSON解析失败: {}", e.getMessage());
        }
        return cases;
    }

    private List<Map<String, Object>> simpleParse(String content) {
        List<Map<String, Object>> cases = new ArrayList<>();
        String[] lines = content.split("\n");

        Map<String, Object> currentCase = null;
        String currentField = null;
        StringBuilder currentValue = new StringBuilder();

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.contains("案件名称") || line.contains("案号") || line.contains("法院") ||
                line.contains("裁判日期") || line.contains("案由") || line.contains("原告") ||
                line.contains("被告") || line.contains("判决") || line.contains("裁判结果")) {

                if (currentCase != null && currentValue.length() > 0) {
                    if (currentField != null) {
                        currentCase.put(currentField, currentValue.toString().trim());
                    }
                    currentValue = new StringBuilder();
                }

                if (currentCase == null) {
                    currentCase = new LinkedHashMap<>();
                }

                if (line.contains("案件名称")) {
                    currentField = "caseName";
                    currentValue.append(extractValue(line, "案件名称"));
                } else if (line.contains("案号")) {
                    currentField = "caseNo";
                    currentValue.append(extractValue(line, "案号"));
                } else if (line.contains("法院")) {
                    currentField = "courtName";
                    currentValue.append(extractValue(line, "法院"));
                } else if (line.contains("裁判日期") || line.contains("判决日期")) {
                    currentField = "judgeDate";
                    currentValue.append(extractValue(line, "裁判日期"));
                } else if (line.contains("案由")) {
                    currentField = "caseCause";
                    currentValue.append(extractValue(line, "案由"));
                } else if (line.contains("原告")) {
                    currentField = "plaintiff";
                    currentValue.append(extractValue(line, "原告"));
                } else if (line.contains("被告")) {
                    currentField = "defendant";
                    currentValue.append(extractValue(line, "被告"));
                } else if (line.contains("裁判结果") || line.contains("判决结果")) {
                    currentField = "judgmentResult";
                    currentValue.append(extractValue(line, "裁判结果"));
                }
            } else if (currentCase != null && currentField != null) {
                if (currentValue.length() > 0) {
                    currentValue.append(" ").append(line);
                }
            }

            if (line.endsWith("。") || line.endsWith("；") || line.endsWith(";")) {
                if (currentCase != null && currentField != null && currentValue.length() > 0) {
                    currentCase.put(currentField, currentValue.toString().trim());
                    currentValue = new StringBuilder();
                    currentField = null;
                }

                if (currentCase != null && !currentCase.isEmpty()) {
                    cases.add(currentCase);
                    currentCase = null;
                }
            }
        }

        if (currentCase != null && !currentCase.isEmpty()) {
            if (currentField != null && currentValue.length() > 0) {
                currentCase.put(currentField, currentValue.toString().trim());
            }
            cases.add(currentCase);
        }

        return cases;
    }

    private String extractValue(String line, String key) {
        int idx = line.indexOf(key);
        if (idx >= 0) {
            String value = line.substring(idx + key.length());
            value = value.replaceAll("^[：:：\\s]+", "");
            value = value.replaceAll("[,，;；。.]+$", "");
            return value.trim();
        }
        return "";
    }

    private Integer parseCaseType(String s) {
        if (s == null || s.isEmpty() || "null".equals(s)) return null;
        s = s.trim();
        if (s.contains("民事")) return 1;
        if (s.contains("刑事")) return 2;
        if (s.contains("行政")) return 3;
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return null;
        }
    }

    private Integer parseCourtLevel(String s) {
        if (s == null || s.isEmpty() || "null".equals(s)) return null;
        s = s.trim();
        if (s.contains("最高")) return 1;
        if (s.contains("高")) return 2;
        if (s.contains("中")) return 3;
        if (s.contains("基层") || s.contains("区") || s.contains("县") || s.contains("市")) return 4;
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return null;
        }
    }

    private Integer parseJudgmentResult(String s) {
        if (s == null || s.isEmpty() || "null".equals(s)) return null;
        s = s.trim();
        if (s.contains("全部支持") || s.contains("支持")) return 1;
        if (s.contains("部分支持")) return 2;
        if (s.contains("驳回")) return 3;
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal parseAmount(String s) {
        if (s == null || s.isEmpty() || "null".equals(s)) return BigDecimal.ZERO;
        try {
            s = s.replaceAll("[^0-9.]", "");
            return new BigDecimal(s);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private java.sql.Date parseDate(String s) {
        if (s == null || s.isEmpty() || "null".equals(s)) return null;
        s = s.trim();
        String[] formats = {"yyyy-MM-dd", "yyyy/MM/dd", "yyyy年MM月dd日", "yyyy.MM.dd"};
        for (String fmt : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(fmt);
                java.util.Date d = sdf.parse(s);
                return new java.sql.Date(d.getTime());
            } catch (ParseException e) { log.debug("Failed to parse date '{}' with format '{}': {}", s, fmt, e.getMessage()); }
        }
        return null;
    }
}
