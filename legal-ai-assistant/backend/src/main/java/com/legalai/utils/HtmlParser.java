package com.legalai.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlParser {

    private static final Pattern USCC_PATTERN = Pattern.compile("([0-9]{18}|\\*{17,})");
    private static final Pattern CAPITAL_PATTERN = Pattern.compile("注册资本[：:]*\\s*([0-9０-９,，.]+)\\s*(?:万|万元|人民币|美元)?");
    private static final Pattern LEGAL_REP_PATTERN = Pattern.compile("(?:法定代表人|法人代表|执行事务合伙人)[：:]*\\s*([^\n\\|,，]{2,15})");
    private static final Pattern ESTABLISH_DATE_PATTERN = Pattern.compile("(?:成立日期|注册日期|开业日期|核准日期)[：:]*\\s*([0-9四〇一二三四五六七八九十]{4}[年\\-/.][0-9〇一二]{1,2}[月\\-/.][0-9〇一二]{1,2}日?)");
    private static final Pattern STATUS_PATTERN = Pattern.compile("(?:经营状态|企业状态|登记状态|状态)[：:]*\\s*([^\n\\|,，\\[\\]]{2,10})");
    private static final Pattern REG_AUTH_PATTERN = Pattern.compile("(?:登记机关|主管部门|发证机关|行政机关)[：:]*\\s*([^\n\\|,，]{2,20})");
    private static final Pattern SHAREHOLDER_PATTERN = Pattern.compile("([^\n，,]{2,15})(?:股东|持股|出资|股权)[^\\n]{0,30}(?:\\d+[．.。]?\\d*%|\\d+%|百分之[一二三四五六七八九十\\d]+)");
    private static final Pattern RISK_PATTERN = Pattern.compile("(?:法律诉讼|被执行人|失信|经营异常|行政处罚)[：:]*\\s*([^\n，,]{5,100})");

    public static String stripTags(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]+>", " ")
                   .replaceAll("&nbsp;", " ")
                   .replaceAll("&amp;", "&")
                   .replaceAll("&lt;", "<")
                   .replaceAll("&gt;", ">")
                   .replaceAll("&quot;", "\"")
                   .replaceAll("\\s+", " ")
                   .trim();
    }

    public static String extractText(String html, int maxLen) {
        String text = stripTags(html);
        if (text.length() > maxLen) {
            text = text.substring(0, maxLen) + "...";
        }
        return text;
    }

    public static String extractUscc(String text) {
        Matcher m = USCC_PATTERN.matcher(text);
        while (m.find()) {
            String match = m.group(1);
            if (match.length() == 18) {
                return match;
            }
        }
        return null;
    }

    public static String extractLegalRepresentative(String text) {
        Matcher m = LEGAL_REP_PATTERN.matcher(text);
        if (m.find()) {
            String rep = m.group(1).trim();
            if (rep.length() >= 2 && rep.length() <= 15) {
                return rep;
            }
        }
        return null;
    }

    public static String extractRegisteredCapital(String text) {
        Matcher m = CAPITAL_PATTERN.matcher(text);
        if (m.find()) {
            String capital = m.group(1);
            capital = capital.replaceAll("[０-９]", "")
                            .replaceAll("[,，]", "")
                            .replaceAll("\\s", "");
            try {
                double val = Double.parseDouble(capital);
                return String.valueOf(val);
            } catch (NumberFormatException e) {
                return capital;
            }
        }
        return null;
    }

    public static String extractEstablishDate(String text) {
        Matcher m = ESTABLISH_DATE_PATTERN.matcher(text);
        if (m.find()) {
            String date = m.group(1);
            date = date.replaceAll("[０-９]", "0");
            date = date.replaceAll("[一二三四五六七八九十]", "0");
            return date;
        }
        return null;
    }

    public static String extractBusinessStatus(String text) {
        Matcher m = STATUS_PATTERN.matcher(text);
        if (m.find()) {
            String status = m.group(1).trim();
            if (status.contains("存续") || status.contains("在业")) return "存续";
            if (status.contains("吊销")) return "吊销";
            if (status.contains("注销")) return "注销";
            if (status.contains("歇业")) return "歇业";
            if (status.contains("停业")) return "停业";
            return status;
        }
        return null;
    }

    public static String extractRegistrationAuthority(String text) {
        Matcher m = REG_AUTH_PATTERN.matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    public static String extractSearchSnippets(String html) {
        String text = stripTags(html);
        StringBuilder sb = new StringBuilder();
        Pattern snippetPattern = Pattern.compile("([^。！？.!?]{50,200}[^。！？.!?\\n]{0,30})");
        Matcher m = snippetPattern.matcher(text);
        int count = 0;
        while (m.find() && count < 10) {
            String snippet = m.group(1).trim();
            if (snippet.length() > 30) {
                sb.append("【片段").append(count + 1).append("】").append(snippet).append("\n\n");
                count++;
            }
        }
        return sb.toString();
    }
}
