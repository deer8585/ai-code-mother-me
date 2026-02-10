package com.wyb.aicodemotherme;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("com.wyb.aicodemotherme.mapper")
public class AiCodeMotherMeApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiCodeMotherMeApplication.class, args);
    }

}
