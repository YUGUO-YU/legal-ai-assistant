package com.legalai.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.legalai.model.SearchLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SearchLogMapper extends BaseMapper<SearchLog> {
}