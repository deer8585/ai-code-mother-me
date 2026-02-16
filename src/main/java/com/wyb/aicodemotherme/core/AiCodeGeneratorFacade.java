package com.wyb.aicodemotherme.core;

import cn.hutool.json.JSONUtil;
import com.wyb.aicodemotherme.ai.AiCodeGeneratorService;
import com.wyb.aicodemotherme.ai.AiCodeGeneratorServiceFactory;
import com.wyb.aicodemotherme.ai.model.message.AiResponseMessage;
import com.wyb.aicodemotherme.ai.model.message.ToolExecutedMessage;
import com.wyb.aicodemotherme.ai.model.message.ToolRequestMessage;
import com.wyb.aicodemotherme.constant.AppConstant;
import com.wyb.aicodemotherme.core.builder.VueProjectBuilder;
import com.wyb.aicodemotherme.core.parser.CodeParserExecutor;
import com.wyb.aicodemotherme.core.saver.CodeFileSaverExecutor;
import com.wyb.aicodemotherme.exception.BusinessException;
import com.wyb.aicodemotherme.exception.ErrorCode;
import com.wyb.aicodemotherme.model.enums.CodeGenTypeEnum;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
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

    @Resource
    private VueProjectBuilder vueProjectBuilder;

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

        //根据appId获取对应的 AI服务实例
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);
        return switch (codeGenTypeEnum) {
            case HTML -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.HTML,appId);
            }
            case MULTI_FILE -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
                yield processCodeStream(codeStream, CodeGenTypeEnum.MULTI_FILE,appId);
            }
            case VUE_PROJECT -> {
                TokenStream tokenStream = aiCodeGeneratorService.generateVueProjectCodeStream(appId,userMessage);
                yield processTokenStream(tokenStream,appId);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 将 TokenStream 转换为 Flux<String>，并传递工具调用信息（返回给前端）并将消息转为json格式的
     *
     * @param tokenStream TokenStream 对象
     * @return Flux<String> 流式响应
     */
    private Flux<String> processTokenStream(TokenStream tokenStream,Long appId) {

        //Flux.create 允许你手动往流里推数据：用 sink.next(...) 推一条，用 sink.complete() 结束，用 sink.error(...) 报错终止。
        return Flux.create(sink -> {
            //AI 每生成一点文本（可能一个 token，也可能一小段），就回调 onPartialResponse。
            //你把 partialResponse 包装成 AiResponseMessage（比如里面 type=ai_response）。
            //再序列化为 JSON 推给 sink.next(...)。输出例如：{type:"ai_response", data:"..."}的格式
            //前端就能边生成边显示，而不是等模型全部说完。
            tokenStream.onPartialResponse((String partialResponse) -> {
                        AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                        //把结构化对象序列化成 JSON推送进 Flux 流,下游（前端 / 消息处理器）会实时收到
                        sink.next(JSONUtil.toJsonStr(aiResponseMessage));
                    })

                    //这是非常关键的一点：
                    //很多 LLM 的工具调用不是一次性把完整 JSON 参数吐出来，而是逐步拼接：
                    //第一段可能只出来 { "relativeFilePath": "src/
                    //后面才补全 main.js", "content": "..." }
                    //所以框架会不断给你“当前阶段的 toolExecutionRequest（可能不完整）”。
                    //你做的事：每来一段就立刻推给前端，让前端能看到：
                    //“模型正在调用工具 writeFile”
                    //“参数正在生成中”
                    //index：当前 tool call 在一次对话中的序号,toolExecutionRequest：当前阶段的工具调用信息（可能不完整）
                    .onPartialToolExecutionRequest((index, toolExecutionRequest) -> {
                        ToolRequestMessage toolRequestMessage = new ToolRequestMessage(toolExecutionRequest);
                        sink.next(JSONUtil.toJsonStr(toolRequestMessage));
                    })

                    //工具真正执行完了（比如文件写入成功/失败），会触发这个回调。
                    //你再推一个 {type:"tool_executed", ...} 给前端。
                    //前端可以用来展示“✅ 已写入 src/main.js”或“❌ 写入失败”。
                    .onToolExecuted((ToolExecution toolExecution) -> {
                        ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolExecution);
                        sink.next(JSONUtil.toJsonStr(toolExecutedMessage));
                    })

                    //这里表达的是一个明确的业务逻辑：
                    //AI 输出结束（包括文本 + 工具调用都结束了）
                    //说明“项目代码生成完毕了”
                    //你接着同步执行 buildProject(...)（npm install + npm run build）
                    //构建完成后再 sink.complete()，告诉前端：流结束了
                    //✅ 优点：前端收到结束信号时，项目已经构建好，预览（dist）目录也已经生成。
                    .onCompleteResponse((ChatResponse response) -> {
                        // 执行 Vue 项目构建（同步执行，确保预览时项目已就绪）
                        String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + "vue_project_" + appId;
                        vueProjectBuilder.buildProject(projectPath);
                        sink.complete();
                    })

                    .onError((Throwable error) -> {
                        error.printStackTrace();
                        sink.error(error);
                    })
                    .start(); //开始监听
        });
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
            .doOnNext(chunk -> {
                // 实时收集代码片段
                codeBuilder.append(chunk);
            })
            .doOnComplete(() -> {
                // 流式返回完成后保存代码
                try {
                    String completeCode = codeBuilder.toString();
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
