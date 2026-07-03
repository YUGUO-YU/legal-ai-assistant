package com.legalai.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.legalai.model.LawCategory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LawCategoryMapper extends BaseMapper<LawCategory> {
}
