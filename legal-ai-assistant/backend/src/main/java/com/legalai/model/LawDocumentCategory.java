package com.legalai.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("law_document_category")
public class LawDocumentCategory {
    private Long lawId;
    private Long categoryId;
}
