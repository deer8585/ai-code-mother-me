package com.wyb.aicodemotherme;

import com.wyb.aicodemotherme.core.AiCodeGeneratorFacade;
import com.wyb.aicodemotherme.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.time.Duration;

@SpringBootTest
class AiCodeMotherMeApplicationTests {

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

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

}
