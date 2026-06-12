package com.legalai.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.legalai.model.LawDocument;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LawDocumentMapper extends BaseMapper<LawDocument> {
}