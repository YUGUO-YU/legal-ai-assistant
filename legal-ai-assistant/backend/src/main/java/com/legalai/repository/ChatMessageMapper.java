package com.legalai.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.legalai.model.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    @Select("SELECT * FROM kb_chat_message WHERE session_uuid = #{sessionUuid} ORDER BY `order` ASC")
    List<ChatMessage> findBySessionUuid(@Param("sessionUuid") String sessionUuid);

    @Select("DELETE FROM kb_chat_message WHERE session_uuid = #{sessionUuid}")
    void deleteBySessionUuid(@Param("sessionUuid") String sessionUuid);
}
