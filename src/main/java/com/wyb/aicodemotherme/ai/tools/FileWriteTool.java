package com.wyb.aicodemotherme.ai.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONObject;
import com.wyb.aicodemotherme.constant.AppConstant;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * 文件写入工具
 * 支持 AI 通过工具调用的方式写入文件
 */
@Slf4j
@Component
public class FileWriteTool extends BaseTool {

    // @Tool --> 表示这个方法可以被 AI 当成“工具”调用
    // "写入文件到指定路径" --> 是给 AI 看的自然语言描述,AI 会根据语义决定是否调用这个工具
    @Tool("写入文件到指定路径")
    public String writeFile(
            @P("文件的相对路径")
            String relativeFilePath,
            @P("要写入的文件内容") // AI 生成的 代码 / 文本内容, 比如 Vue、JS、HTML、JSON 等
            String content, //AI 决定的要传什么内容
            // @ToolMemeryId --> LangChain4j 提供的特殊注解,自动把当前对话的 memoryId 注入进来
            // appId --> 一个 AI 生成项目的唯一标识
            @ToolMemoryId Long appId
    ) {
        try {
            Path path = Paths.get(relativeFilePath); //把字符串路径转换为 Path

            //1. 判断是否是绝对路径,不是则把它拼接成绝对路径
            if(!path.isAbsolute()){
                String projectDirName = "vue_project_" + appId; //构造项目目录名
                Path projectRoot = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, projectDirName); //项目根目录
                path = projectRoot.resolve(relativeFilePath); //拼接最终文件路径（projectRoot + relativeFilePath）
            }

            //2. 创建父目录 (文件所在目录)如果不存在, 保证多级目录一次性创建成功
            Path parentDir = path.getParent();
            if(parentDir != null){
                Files.createDirectories(parentDir);
            }
            //写入文件内容
            Files.write(path, content.getBytes(),
                    StandardOpenOption.CREATE, //文件不存在 → 创建
                    StandardOpenOption.TRUNCATE_EXISTING //文件存在 → 清空再写（覆盖）
            );
            log.info("成功写入文件: {}", path.toAbsolutePath());
            return "文件写入成功:{}" + relativeFilePath;
        } catch (IOException e) {
            String errorMessage = "文件写入失败：" + relativeFilePath + ",错误：" + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }

    @Override
    public String getToolName() {
        return "writeFile";
    }

    @Override
    public String getDisplayName() {
        return "写入文件";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        String relativeFilePath = arguments.getStr("relativeFilePath"); //例如"main/src/main.js"
        String suffix = FileUtil.getSuffix(relativeFilePath); //取后缀，例如“.js"
        String content = arguments.getStr("content"); //写入内容
        return String.format("""
                        [工具调用] %s %s
                        ```%s
                        %s
                        ```
                        """, getDisplayName(), relativeFilePath, suffix, content);
    }
}
