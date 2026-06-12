package com.legalai.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.legalai.model.LawArticle;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LawArticleMapper extends BaseMapper<LawArticle> {
}