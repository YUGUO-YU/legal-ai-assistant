package com.legalai.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.legalai.model.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    @Select("SELECT * FROM kb_chat_message WHERE session_uuid = #{sessionUuid} ORDER BY `order` ASC")
    List<ChatMessage> findBySessionUuid(@Param("sessionUuid") String sessionUuid);

    @Select("DELETE FROM kb_chat_message WHERE session_uuid = #{sessionUuid}")
    void deleteBySessionUuid(@Param("sessionUuid") String sessionUuid);

    @Select("SELECT session_uuid as sessionId, user_id as userId, MIN(created_at) as firstMessage, MAX(created_at) as lastMessage, COUNT(*) as messageCount FROM kb_chat_message WHERE user_id = #{userId} GROUP BY session_uuid, user_id ORDER BY lastMessage DESC")
    List<Map<String, Object>> findSessionsByUserId(@Param("userId") String userId);
}
