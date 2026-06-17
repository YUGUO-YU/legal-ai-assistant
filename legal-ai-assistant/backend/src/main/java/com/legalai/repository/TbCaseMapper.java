package com.legalai.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.legalai.model.TbCase;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TbCaseMapper extends BaseMapper<TbCase> {
}
