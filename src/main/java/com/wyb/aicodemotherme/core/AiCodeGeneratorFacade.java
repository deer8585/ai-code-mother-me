package com.wyb.aicodemotherme.core;

import com.wyb.aicodemotherme.ai.AiCodeGeneratorServiceFactory;
import com.wyb.aicodemotherme.core.parser.CodeParserExecutor;
import com.wyb.aicodemotherme.core.saver.CodeFileSaverExecutor;
import com.wyb.aicodemotherme.exception.BusinessException;
import com.wyb.aicodemotherme.exception.ErrorCode;
import com.wyb.aicodemotherme.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
/**
 * AI 代码生成类，组合生成和保存功能
 */
@Slf4j
@Service
public class AiCodeGeneratorFacade {

    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;

    /**
     * 统一入口：根据类型生成并保存代码（流式）
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum,Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> {
                Flux<String> codeStream = aiCodeGeneratorServiceFactory.aiCodeGeneratorService().generateHtmlCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.HTML,appId);
            }
            case MULTI_FILE -> {
                Flux<String> codeStream = aiCodeGeneratorServiceFactory.aiCodeGeneratorService().generateMultiFileCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.MULTI_FILE,appId);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }


    /**
     * 通用流式代码处理方法
     *
     * @param codeStream  代码流
     * @param codeGenType 代码生成类型
     * @return 流式响应
     */
    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenType,Long appId) {
        StringBuilder codeBuilder = new StringBuilder();
        return codeStream
            .doOnSubscribe(subscription -> log.info("开始接收AI流式响应..."))
            .doOnNext(chunk -> {
                // 实时收集代码片段
                log.debug("接收到代码片段: {}", chunk);
                codeBuilder.append(chunk);
            })
            .doOnComplete(() -> {
                // 流式返回完成后保存代码
                log.info("AI响应完成，开始保存代码...");
                try {
                    String completeCode = codeBuilder.toString();
                    log.info("完整代码长度: {} 字符", completeCode.length());
                    
                    // 使用执行器解析代码
                    Object parsedResult = CodeParserExecutor.executeParser(completeCode, codeGenType);
                    // 使用执行器保存代码
                    File savedDir = CodeFileSaverExecutor.executeSaver(parsedResult, codeGenType,appId);
                    log.info("保存成功，路径为：{}", savedDir.getAbsolutePath());
                } catch (Exception e) {
                    log.error("保存失败: {}", e.getMessage(), e);
                }
            })
            .doOnError(error -> log.error("流式处理出错: {}", error.getMessage(), error));
    }

}
