package com.legalai.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.legalai.model.LawDocumentCategory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LawDocumentCategoryMapper extends BaseMapper<LawDocumentCategory> {
}
