package com.wyb.aicodemotherme.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

public interface AiCodeGeneratorService {

/*    *//**
     * 生成 HTML 代码
     * @param userMessage
     * @return
     *//*
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    HtmlCodeResult generateHtmlCode(String userMessage);

    *//**
     * 生成 多文件 代码
     * @param userMessage
     * @return
     *//*
    @SystemMessage(fromResource = "prompt/codegen-multi-file-system-prompt.txt")
    MultiFileCodeResult generateMultiFileCode(String userMessage);*/

    /**
     * 生成 HTML 代码 (流式)
     * @param userMessage 用户消息
     * @return
     */
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    Flux<String> generateHtmlCodeStream( String userMessage);

    /**
     * 生成 多文件 代码 (流式)
     * @param userMessage 用户消息
     * @return
     */
    @SystemMessage(fromResource = "prompt/codegen-multi-file-system-prompt.txt")
    Flux<String> generateMultiFileCodeStream( String userMessage);

    /**
     * 生成 vue项目 代码 (流式)
     * @param appId 应用Id
     * @param userMessage 用户提示词
     * @return AI 输出结果
     */
//    @SystemMessage(fromResource = "prompt/codegen-vue-project-system-prompt.txt")
//    TokenStream generateVueProjectCodeStream(@MemoryId Long appId, @UserMessage String userMessage);

}
