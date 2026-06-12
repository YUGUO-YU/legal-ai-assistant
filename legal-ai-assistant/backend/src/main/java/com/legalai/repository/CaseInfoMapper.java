package com.legalai.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.legalai.model.CaseInfo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CaseInfoMapper extends BaseMapper<CaseInfo> {
}