package com.legalai.service;

import com.legalai.dto.PptDocumentDTO;
import com.legalai.dto.SlideDTO;
import org.apache.poi.xslf.usermodel.*;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.awt.Rectangle;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

@Component
public class PptxGenerator {

    private static final Map<String, TemplateStyle> TEMPLATE_STYLES = Map.of(
            "legal-blue", new TemplateStyle("1a365d", "2c5282", true),
            "purple-peak", new TemplateStyle("553c9a", "805ad5", true),
            "professional", new TemplateStyle("2d3748", "4a5568", false),
            "fresh-minimal", new TemplateStyle("319795", "38b2ac", false),
            "court-gold", new TemplateStyle("744210", "d69e2e", true)
    );

    public byte[] generate(PptDocumentDTO document) {
        try (XMLSlideShow pptx = new XMLSlideShow()) {
            pptx.setPageSize(new java.awt.Dimension(13, 7));

            TemplateStyle style = TEMPLATE_STYLES.getOrDefault(document.getTemplateId(),
                    TEMPLATE_STYLES.get("legal-blue"));

            if (document.getSlides() == null || document.getSlides().isEmpty()) {
                addDefaultSlide(pptx, document.getTitle(), style);
            } else {
                for (SlideDTO slide : document.getSlides()) {
                    addSlide(pptx, slide, style);
                }
            }

            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                pptx.write(out);
                return out.toByteArray();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PPTX", e);
        }
    }

    private void addSlide(XMLSlideShow pptx, SlideDTO slide, TemplateStyle style) {
        XSLFSlide xmlSlide = pptx.createSlide();

        Color primaryColor = Color.decode("#" + style.primaryColor);
        Color secondaryColor = Color.decode("#" + style.secondaryColor);

        if ("title_only".equals(slide.getLayout())) {
            addCoverSlide(xmlSlide, slide, primaryColor, secondaryColor, style);
        } else {
            addContentSlide(xmlSlide, slide, primaryColor);
        }

        if (slide.getNotes() != null && !slide.getNotes().isEmpty()) {
            addNotes(xmlSlide, slide);
        }
    }

    private void addCoverSlide(XSLFSlide slide, SlideDTO slideDTO,
                               Color primaryColor, Color secondaryColor, TemplateStyle style) {
        XSLFTextBox titleBox = slide.createTextBox();
        titleBox.setAnchor(new Rectangle(700, 2000, 5400, 1200));

        XSLFTextParagraph titlePara = titleBox.addNewTextParagraph();
        titlePara.setTextAlign(XSLFTextParagraph.TextAlign.CENTER);

        XSLFTextRun titleRun = titlePara.addNewTextRun();
        titleRun.setText(slideDTO.getTitle());
        titleRun.setFontSize(44.0);
        titleRun.setFontColor(Color.WHITE);
        titleRun.setBold(true);

        if (slideDTO.getBulletPoints() != null && !slideDTO.getBulletPoints().isEmpty()) {
            XSLFTextBox contentBox = slide.createTextBox();
            contentBox.setAnchor(new Rectangle(1000, 3200, 5000, 2000));

            for (String point : slideDTO.getBulletPoints()) {
                XSLFTextParagraph para = contentBox.addNewTextParagraph();
                para.setTextAlign(XSLFTextParagraph.TextAlign.CENTER);

                XSLFTextRun run = para.addNewTextRun();
                run.setText(point);
                run.setFontSize(20.0);
                run.setFontColor(Color.WHITE);
            }
        }
    }

    private void addContentSlide(XSLFSlide slide, SlideDTO slideDTO, Color primaryColor) {
        XSLFTextBox titleBox = slide.createTextBox();
        titleBox.setAnchor(new Rectangle(500, 300, 8000, 800));

        XSLFTextParagraph titlePara = titleBox.addNewTextParagraph();
        titlePara.setTextAlign(XSLFTextParagraph.TextAlign.LEFT);

        XSLFTextRun titleRun = titlePara.addNewTextRun();
        titleRun.setText(slideDTO.getTitle());
        titleRun.setFontSize(32.0);
        titleRun.setFontColor(primaryColor);
        titleRun.setBold(true);

        if (slideDTO.getBulletPoints() != null) {
            XSLFTextBox contentBox = slide.createTextBox();
            contentBox.setAnchor(new Rectangle(500, 1200, 8000, 5000));

            for (String point : slideDTO.getBulletPoints()) {
                XSLFTextParagraph para = contentBox.addNewTextParagraph();
                para.setSpaceBefore(14.0);

                XSLFTextRun bulletRun = para.addNewTextRun();
                bulletRun.setText("\u2022 " + point);
                bulletRun.setFontSize(18.0);
                bulletRun.setFontColor(Color.DARK_GRAY);
            }
        }
    }

    private void addNotes(XSLFSlide slide, SlideDTO slideDTO) {
        try {
            XSLFNotes notes = slide.getNotes();
            if (notes != null) {
                XSLFTextBox notesBox = notes.createTextBox();
                notesBox.addNewTextParagraph().addNewTextRun().setText(slideDTO.getNotes());
            }
        } catch (Exception e) {
        }
    }

    private void addDefaultSlide(XMLSlideShow pptx, String title, TemplateStyle style) {
        XSLFSlide slide = pptx.createSlide();

        XSLFTextBox titleBox = slide.createTextBox();
        titleBox.setAnchor(new Rectangle(700, 2500, 5400, 1200));

        XSLFTextParagraph para = titleBox.addNewTextParagraph();
        para.setTextAlign(XSLFTextParagraph.TextAlign.CENTER);
        XSLFTextRun run = para.addNewTextRun();
        run.setText(title);
        run.setFontSize(44.0);
        run.setFontColor(Color.WHITE);
        run.setBold(true);
    }

    private static class TemplateStyle {
        final String primaryColor;
        final String secondaryColor;
        final boolean isGradient;

        TemplateStyle(String primaryColor, String secondaryColor, boolean isGradient) {
            this.primaryColor = primaryColor;
            this.secondaryColor = secondaryColor;
            this.isGradient = isGradient;
        }
    }
}
