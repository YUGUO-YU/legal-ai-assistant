package com.legalai.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class IdGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    public static String generateUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String generateLawUuid() {
        return "LAW-" + generateDatePrefix() + "-" + generateRandomSuffix(6);
    }

    public static String generateArticleUuid() {
        return "ART-" + generateDatePrefix() + "-" + generateRandomSuffix(6);
    }

    public static String generateCaseUuid() {
        return "CASE-" + generateDatePrefix() + "-" + generateRandomSuffix(6);
    }

    public static String generateKbUuid() {
        return "KB-" + generateDatePrefix() + "-" + generateRandomSuffix(6);
    }

    public static String generateDocUuid() {
        return "DOC-" + generateDatePrefix() + "-" + generateRandomSuffix(6);
    }

    public static String generateSessionUuid() {
        return "SESSION-" + generateDatePrefix() + "-" + generateRandomSuffix(6);
    }

    private static String generateDatePrefix() {
        return LocalDateTime.now().format(FORMATTER);
    }

    private static String generateRandomSuffix(int length) {
        String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * chars.length());
            sb.append(chars.charAt(index));
        }
        return sb.toString();
    }
}