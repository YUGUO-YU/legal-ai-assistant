package com.legalai.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
public class DocumentParserService {

    private static final Logger log = LoggerFactory.getLogger(DocumentParserService.class);

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;

    public String parseDocument(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件大小超过限制(50MB)");
        }

        String extension = getFileExtension(fileName).toLowerCase();

        return switch (extension) {
            case "pdf" -> parsePdf(file.getInputStream());
            case "docx" -> parseDocx(file.getInputStream());
            case "doc" -> parseDoc(file.getInputStream());
            case "txt" -> parseTxt(file.getInputStream());
            default -> throw new IllegalArgumentException("不支持的文件格式: " + extension);
        };
    }

    private String parsePdf(InputStream inputStream) throws IOException {
        log.info("解析PDF文件");
        try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            log.info("PDF解析完成，字符数: {}", text.length());
            return cleanText(text);
        }
    }

    private String parseDocx(InputStream inputStream) throws IOException {
        log.info("解析DOCX文件");
        try (XWPFDocument document = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            String text = extractor.getText();
            log.info("DOCX解析完成，字符数: {}", text.length());
            return cleanText(text);
        }
    }

    private String parseDoc(InputStream inputStream) throws IOException {
        log.warn("DOC格式解析受限，返回原始文本");
        return parseTxt(inputStream);
    }

    private String parseTxt(InputStream inputStream) throws IOException {
        log.info("解析TXT文件");
        String text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        log.info("TXT解析完成，字符数: {}", text.length());
        return cleanText(text);
    }

    private String cleanText(String text) {
        if (text == null) return "";
        return text.replaceAll("\\s+", " ")
                   .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "")
                   .trim();
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot < 0) {
            return "";
        }
        return fileName.substring(lastDot + 1);
    }
}
