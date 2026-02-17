package com.wyb.aicodemotherme.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.wyb.aicodemotherme.ai.guardrail.PromptSafetyInputGuardrail;
import com.wyb.aicodemotherme.ai.tools.ToolManager;
import com.wyb.aicodemotherme.exception.BusinessException;
import com.wyb.aicodemotherme.exception.ErrorCode;
import com.wyb.aicodemotherme.model.enums.CodeGenTypeEnum;
import com.wyb.aicodemotherme.service.ChatHistoryService;
import com.wyb.aicodemotherme.util.SpringContextUtil;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * AI 代码生成服务工厂（生产 AI service 的）
 */
@Slf4j
@Configuration
public class AiCodeGeneratorServiceFactory {


    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private ToolManager toolManager;


    /**
     * 根据appId 和 codeGenType（ 生成类型）获取 AI 代码生成服务
     * @return
     */

    private AiCodeGeneratorService createAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        log.debug("为appId:{}创建新的AI服务实例", appId);
        //1. 根据 appId 构建独立的对话记忆（下次对话时把存储的对话带上去)
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder() //负责“保留最近 N 条”的策略（怎么裁剪、怎么取窗口）
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore) //负责“把消息存哪里”的存储层接口（MemoryStore）
                .maxMessages(20)
                .build();

        //2. 从数据库加载历史对话到记忆中
        chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 20);

        //3. 根据代码生成类型选择不同的模型配置
        return switch (codeGenType) {
            //Vue项目使用推理模型
            case VUE_PROJECT -> {
                // 使用多例模式的 StreamingChatModel 解决并发问题
                StreamingChatModel reasoningStreamingChatModel = SpringContextUtil.getBean(
                        "reasoningStreamingChatModelPrototype", StreamingChatModel.class);
                yield AiServices.builder(AiCodeGeneratorService.class)

                        .streamingChatModel(reasoningStreamingChatModel)
                        //为工具（ToolMemoryId）提供上下文能力,这是 AI 工具调用能拿到 appId 的前提
                        .chatMemoryProvider(memoryId -> chatMemory) //支持 memoryId → chatMemory 的映射
                        .tools(toolManager.getAllTools())
                        .inputGuardrails(new PromptSafetyInputGuardrail()) // 添加输入护轨
                        //防 AI 幻觉
                        .hallucinatedToolNameStrategy(toolExecutionRequest ->
                                ToolExecutionResultMessage.from(toolExecutionRequest,
                                        "Error: there is no tool called" +
                                                toolExecutionRequest.name()))
                        .maxSequentialToolsInvocations(20) // 最多连续调用 20 次工具
                        .build();
            }


            //HTML和多文件生成使用默认模型
            case HTML, MULTI_FILE -> {
                // 使用多例模式的 StreamingChatModel 解决并发问题
                StreamingChatModel openAiStreamingChatModel = SpringContextUtil.getBean(
                        "streamingChatModelPrototype", StreamingChatModel.class);
                yield AiServices.builder(AiCodeGeneratorService.class)
                        .streamingChatModel(openAiStreamingChatModel)
                        .chatMemory(chatMemory)
                        .inputGuardrails(new PromptSafetyInputGuardrail()) // 添加输入护轨
                        .maxSequentialToolsInvocations(20) // 最多连续调用 20 次工具
                        .build();
            }

            default -> throw new BusinessException(
                    ErrorCode.SYSTEM_ERROR,
                    "不支持的代码生成类型: " + codeGenType.getValue()
            );
        };
    }

    /**
     * AI服务实例缓存 (Caffeine)
     * 缓存策略
     * 最大缓存1000个实例
     * 写入后 30分钟过期
     * 访问后 10分钟过期
     */
    //定义一个成员变量 serviceCache
    //用 Caffeine 创建缓存（本地内存缓存库，性能很好）
    private static Cache<String, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000) //缓存最多存 1000 个服务实例
            .expireAfterWrite(Duration.ofMinutes(30)) //写入后 30 分钟过期
            .expireAfterAccess(Duration.ofMinutes(10)) //10 分钟不访问就过期
            .removalListener((key, value, cause) -> { //设置移除监听器
                log.debug("AI服务实例被移除，cacheKey:{},原因:{}", key, cause);
            })
            .build();



    /**
     * 根据 cacheKey 获取 AI 服务（带缓存）
     * @param appId
     * @return
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId,CodeGenTypeEnum codeGenType) {
        String cacheKey = buildCacheKey(appId,codeGenType);
        //如果能找到则直接返回，找不到则创建一个cacheKey缓存
        return serviceCache.get(cacheKey,key -> createAiCodeGeneratorService(appId,codeGenType));
    }

    /**
     * 构建 caffeine 缓存键
     * @param appId
     * @param codeGenType
     * @return
     */
    private String buildCacheKey(long appId, CodeGenTypeEnum codeGenType) {
        return appId + "-" + codeGenType.getValue();
    }

    /**
     * 清理所有缓存（用于解决类加载器冲突问题）
     */
    public void clearAllCache() {
        serviceCache.invalidateAll();
        log.info("已清理所有AI服务实例缓存");
    }
}
