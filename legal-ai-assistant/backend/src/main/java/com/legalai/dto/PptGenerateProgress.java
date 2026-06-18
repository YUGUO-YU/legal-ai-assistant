package com.legalai.dto;

import lombok.Data;
import java.util.List;

@Data
public class PptGenerateProgress {
    private String stage;
    private int progress;
    private String message;
    private List<SlideDTO> slides;
    private boolean completed;
    private String error;
    private String pptId;

    public static PptGenerateProgress stage(String stage, int progress, String message) {
        PptGenerateProgress p = new PptGenerateProgress();
        p.stage = stage;
        p.progress = progress;
        p.message = message;
        p.completed = false;
        return p;
    }

    public static PptGenerateProgress done(String pptId, List<SlideDTO> slides) {
        PptGenerateProgress p = new PptGenerateProgress();
        p.stage = "done";
        p.progress = 100;
        p.message = "PPT 生成完成";
        p.completed = true;
        p.pptId = pptId;
        p.slides = slides;
        return p;
    }

    public static PptGenerateProgress error(String error) {
        PptGenerateProgress p = new PptGenerateProgress();
        p.stage = "error";
        p.progress = 0;
        p.message = "生成失败";
        p.completed = true;
        p.error = error;
        return p;
    }
}
