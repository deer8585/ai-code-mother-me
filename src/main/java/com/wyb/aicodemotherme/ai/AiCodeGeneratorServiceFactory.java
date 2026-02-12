package com.wyb.aicodemotherme.ai;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
/**
 * AI 代码生成服务工厂
 */
@Slf4j
@Configuration
public class AiCodeGeneratorServiceFactory {

    @Resource
    private ChatModel chatModel;

    @Resource
    private StreamingChatModel streamingChatModel;

    /**
     * 构建一个与AI大模型对话的 bean
     * @return
     */
    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService() {
        // 创建聊天记录存储
        ChatMemoryStore chatMemoryStore = new InMemoryChatMemoryStore();
        
        return AiServices.builder(AiCodeGeneratorService.class)
                .streamingChatModel(streamingChatModel)
//                .chatModel(chatModel)
//                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
//                        .id(memoryId)
//                        .maxMessages(20) // 保留最近20条消息
//                        .chatMemoryStore(chatMemoryStore)
//                        .build())
                .build();
    }
}
