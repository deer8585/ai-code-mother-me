package com.wyb.aicodemotherme;

import com.wyb.aicodemotherme.core.AiCodeGeneratorFacade;
import com.wyb.aicodemotherme.model.entity.User;
import com.wyb.aicodemotherme.model.enums.CodeGenTypeEnum;
import com.wyb.aicodemotherme.service.AppService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;

@SpringBootTest
class AiCodeMotherMeApplicationTests {

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Resource
    private AppService appService;

    @Test
    void contextLoads() {
        Flux<String> resultStream = aiCodeGeneratorFacade.generateAndSaveCodeStream(
            "生成一个任务记录网站，不多于20行代码",
            CodeGenTypeEnum.HTML,
                2022006075610714114L
        );
        
        // 订阅并等待完成，设置超时时间为60秒
        resultStream
            .doOnNext(chunk -> System.out.print(chunk)) // 实时打印AI回复
            .doOnComplete(() -> System.out.println("\n=== 代码生成完成 ==="))
            .doOnError(error -> System.err.println("生成失败: " + error.getMessage()))
            .blockLast(Duration.ofSeconds(60)); // 阻塞等待完成，最多等待60秒
    }

    @Test
    void chatToGenCode(){
        Flux<String> stringFlux = appService.chatToGenCode(2022006075610714114L, "修改标题",
                User.builder()
                        .id(2020841788611518465L)
                        .userName("deer")
                        .userAccount("deer")
                        .userPassword("12345678")
                        .userRole("admin").build());

        stringFlux
                .doOnNext(chunk -> System.out.print(chunk)) // 实时打印AI回复
                .doOnComplete(() -> System.out.println("\n=== 代码生成完成 ==="))
                .doOnError(error -> System.err.println("生成失败: " + error.getMessage()))
                .blockLast(Duration.ofSeconds(60)); // 阻塞等待完成，最多等待60秒
    }

    @Test
    void generateVueProjectCodeStream() {
        Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream(
                "简单的任务记录网站，总代码量不超过 200 行",
                CodeGenTypeEnum.VUE_PROJECT, 1L);
        codeStream
                .doOnNext(chunk -> System.out.print(chunk)) // 实时打印AI回复
                .doOnComplete(() -> System.out.println("\n=== 代码生成完成 ==="))
                .doOnError(error -> System.err.println("生成失败: " + error.getMessage()))
                .blockLast(Duration.ofSeconds(60)); // 阻塞等待流完成，最多等待60秒
//        // 阻塞等待所有数据收集完成
//        List<String> result = codeStream.collectList().block();
//        // 验证结果
//        Assertions.assertNotNull(result);
//        String completeContent = String.join("", result);
//        Assertions.assertNotNull(completeContent);
    }


}
