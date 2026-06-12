package com.legalai.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.legalai.model.LegalCase;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LegalCaseMapper extends BaseMapper<LegalCase> {
}