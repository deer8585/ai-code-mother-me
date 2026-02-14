package com.wyb.aicodemotherme.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wyb.aicodemotherme.constant.UserConstant;
import com.wyb.aicodemotherme.exception.BusinessException;
import com.wyb.aicodemotherme.exception.ErrorCode;
import com.wyb.aicodemotherme.exception.ThrowUtils;
import com.wyb.aicodemotherme.model.dto.chathistory.ChatHistoryQueryRequest;
import com.wyb.aicodemotherme.model.entity.App;
import com.wyb.aicodemotherme.model.entity.ChatHistory;
import com.wyb.aicodemotherme.model.entity.User;
import com.wyb.aicodemotherme.model.enums.ChatHistoryMessageTypeEnum;
import com.wyb.aicodemotherme.service.AppService;
import com.wyb.aicodemotherme.service.ChatHistoryService;
import com.wyb.aicodemotherme.mapper.ChatHistoryMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话历史 服务层实现。
 *
 * @author wyb
 */
@Slf4j
@Service
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory> implements ChatHistoryService{

    @Resource
    @Lazy
    private AppService appService;

    /**
     * 添加对话历史
     * @param appId 应用id
     * @param message 消息
     * @param messageType 消息类型
     * @param userId 用户id
     * @return
     */
    @Override
    public boolean addChatMessage(Long appId, String message, String messageType, Long userId) {
        //1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "appId不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "消息内容不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(messageType), ErrorCode.PARAMS_ERROR, "消息类型不能为空");
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户id不能为空");

        //2. 验证消息类型是否有效
        ChatHistoryMessageTypeEnum messageTypeEnum = ChatHistoryMessageTypeEnum.getEnumByValue(messageType);
        ThrowUtils.throwIf(messageTypeEnum == null, ErrorCode.PARAMS_ERROR, "不支持的消息类型" + messageType);

        //3. 构建对话消息历史
        ChatHistory chatHistory = new ChatHistory();
        chatHistory.setAppId(appId);
        chatHistory.setMessage(message);
        chatHistory.setMessageType(messageType);
        chatHistory.setUserId(userId);
        return this.save(chatHistory);

    }

    /**
     * 根据应用 ID 删除应用的历史对话
     * @param appId 应用 ID
     * @return
     */
    @Override
    public boolean deleteByAppId(Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        LambdaQueryWrapper<ChatHistory> queryWrapper = new QueryWrapper<ChatHistory>()
                .lambda().eq(ChatHistory::getAppId, appId);
        return this.remove(queryWrapper);
    }



    /**
     * 构造QueryWrapper对象生成SQL查询
     * @param chatHistoryQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<ChatHistory> getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest) {
        if (chatHistoryQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }

        Long id = chatHistoryQueryRequest.getId();
        String message = chatHistoryQueryRequest.getMessage();
        String messageType = chatHistoryQueryRequest.getMessageType();
        Long appId = chatHistoryQueryRequest.getAppId();
        Long userId = chatHistoryQueryRequest.getUserId();
        LocalDateTime lastCreateTime = chatHistoryQueryRequest.getLastCreateTime();
        String sortField = chatHistoryQueryRequest.getSortField();
        String sortOrder = chatHistoryQueryRequest.getSortOrder();

        QueryWrapper<ChatHistory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(messageType), "messageType", messageType);
        queryWrapper.eq(ObjUtil.isNotNull(appId), "appId", appId);
        queryWrapper.eq(ObjUtil.isNotNull(userId), "userId", userId);

        //如果 appName 有值，则拼 AND appName LIKE '%xxx%'（MP 默认会自动加 %）
        queryWrapper.like(StrUtil.isNotBlank(message), "message", message);

        // 游标查询逻辑 - 只使用 createTime 作为游标
        if (lastCreateTime != null) {
            queryWrapper.lt("createTime", lastCreateTime);
        }
        //StrUtil.isNotEmpty(sortField)：作为 condition
        //sortField 不为空才会排序，否则不拼 ORDER BY
        //有 sortField 就按它排；没有就默认按创建时间倒序排
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        // 排序
        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(true,"ascend".equals(sortOrder), sortField);
        } else {
            // 默认按创建时间降序排列（最新的在前）
            queryWrapper.orderByDesc("createTime");
        }
        return queryWrapper;
    }

    /**
     * 分页获取应用的历史对话（游标查询）
     * @param appId 应用 ID
     * @param lastCreateTime 最后创建时间
     * @param pageSize 页面大小
     * @param loginUser 登录用户
     * @return
     */
    @Override
    public Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                                      LocalDateTime lastCreateTime,
                                                      User loginUser) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > 50, ErrorCode.PARAMS_ERROR, "页面大小必须在1-50之间");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        // 验证权限：只有应用创建者和管理员可以查看
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        boolean isCreator = app.getUserId().equals(loginUser.getId());
        ThrowUtils.throwIf(!isAdmin && !isCreator, ErrorCode.NO_AUTH_ERROR, "无权查看该应用的对话历史");
        // 构建查询条件
        ChatHistoryQueryRequest queryRequest = new ChatHistoryQueryRequest();
        queryRequest.setAppId(appId);
        queryRequest.setLastCreateTime(lastCreateTime);
        QueryWrapper queryWrapper = this.getQueryWrapper(queryRequest);
        // 查询数据
        //因为这里是“游标分页”（lastCreateTime），不是传统 pageNum=2/3 那种翻页。
        //每次都从“游标点之前”取最新的一批，所以页码永远是 1。
        return this.page(Page.of(1, pageSize), queryWrapper);
    }

    /**
     * 加载对话历史到内存
     * @param appId
     * @param chatMemory
     * @param maxCount
     * @return 加载成功的条数
     */
    @Override
    public int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount){

        try {
            //1. 构造查询条件，起始点为1用于排除最新的用户消息 (取最新的若干条，但排除最新的一条)
            LambdaQueryWrapper<ChatHistory> queryWrapper = Wrappers.lambdaQuery(ChatHistory.class)
                    .eq(ChatHistory::getAppId, appId)
                    .orderByDesc(ChatHistory::getCreateTime);// 降序排，获取最新的几条信息
            Page<ChatHistory> page = new Page<>(1, maxCount + 1); // 多查 1 条
            List<ChatHistory> historyList = this.list(page,queryWrapper);
             // 跳过最新 1 条
            if (!historyList.isEmpty()) {
                historyList.remove(0);
            }
            if(CollUtil.isEmpty(historyList)){
                return 0;
            }

            //2. 反转列表，确保按时间正序（老的在前，新的在后）
            historyList = historyList.reversed();

            //3. 按时间顺序添加到记忆(chatMemory)中
            int loadedCount = 0;
            //3.1 先清理历史缓存，防止重复加载（可能会有之前缓存在 Redis 和 Caffeine 的还没过期）
            chatMemory.clear();

            //3.2 遍历取到的消息，加载到历史对话中(chatMemory)
            for (ChatHistory chatHistory : historyList) {
                if(ChatHistoryMessageTypeEnum.USER.equals(chatHistory.getMessageType())){
                    //是用户消息,使用langchain4j的UserMessage存储
                    chatMemory.add(UserMessage.from(chatHistory.getMessage()));
                    loadedCount++;
                }else {
                    if (ChatHistoryMessageTypeEnum.AI.equals(chatHistory.getMessageType())) {
                        //是ai消息，使用langchain4j的AiMessage存储
                        chatMemory.add(AiMessage.from(chatHistory.getMessage()));
                        loadedCount++;
                    }
                }

            }

            log.info("成功为appId:{} 加载了 {} 条历史消息", appId, loadedCount);
            return loadedCount;
        }catch (Exception e){
            log.error("加载历史对话失败,appId : {},error : {}", appId, e.getMessage());
            return 0;
        }

    }
}




