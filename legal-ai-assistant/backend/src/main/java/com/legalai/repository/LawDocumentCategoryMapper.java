package com.legalai.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.legalai.model.LawDocumentCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Insert;

import java.util.List;

@Mapper
public interface LawDocumentCategoryMapper extends BaseMapper<LawDocumentCategory> {
    @Insert("<script>" +
            "INSERT INTO law_document_category (law_id, category_id) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.lawId}, #{item.categoryId})" +
            "</foreach>" +
            "</script>")
    void batchInsert(@Param("list") List<LawDocumentCategory> list);
}
