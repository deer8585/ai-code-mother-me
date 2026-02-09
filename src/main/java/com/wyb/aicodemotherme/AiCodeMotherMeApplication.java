package com.wyb.aicodemotherme;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
public class AiCodeMotherMeApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiCodeMotherMeApplication.class, args);
    }

}
