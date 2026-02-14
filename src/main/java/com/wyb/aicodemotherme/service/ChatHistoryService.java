package com.wyb.aicodemotherme.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wyb.aicodemotherme.model.dto.chathistory.ChatHistoryQueryRequest;
import com.wyb.aicodemotherme.model.entity.ChatHistory;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wyb.aicodemotherme.model.entity.User;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.time.LocalDateTime;

/**
* @author 28442
* @description 针对表【chat_history(对话历史)】的数据库操作Service
* @createDate 2026-02-14 00:55:32
*/
public interface ChatHistoryService extends IService<ChatHistory> {

    /**
     * 添加对话历史
     * @param appId 应用id
     * @param message 消息
     * @param messageType 消息类型
     * @param userId 用户id
     * @return
     */
    boolean addChatMessage(Long appId, String message, String messageType, Long userId);

    /**
     * 根据应用 ID 删除应用的历史对话
     * @param appId 应用 ID
     * @return
     */
    boolean deleteByAppId(Long appId);

    /**
     * 构造QueryWrapper对象生成SQL查询
     * @param chatHistoryQueryRequest
     * @return
     */
    QueryWrapper<ChatHistory> getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest);

    /**
     * 分页获取应用的历史对话（游标查询）
     * @param appId 应用 ID
     * @param lastCreateTime 最后创建时间
     * @param pageSize 页面大小
     * @param loginUser 登录用户
     * @return
     */
    Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                               LocalDateTime lastCreateTime,
                                               User loginUser);

    /**
     * 加载对话历史到内存
     * @param appId
     * @param chatMemory
     * @param maxCount
     * @return 加载成功的条数
     */
    int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount);
}
