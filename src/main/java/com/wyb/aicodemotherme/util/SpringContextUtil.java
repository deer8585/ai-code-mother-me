package com.wyb.aicodemotherme.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * 获取多例bean
 */
//通过 ApplicationContextAware 在启动时拿到 ApplicationContext
//保存到静态变量里
//提供 getBean(...) 的静态方法随时取 Bean
@Component
public class SpringContextUtil implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    //Spring 启动创建这个 SpringContextUtil Bean 时，会调用该方法，把容器 ApplicationContext 传进来。
    //把它保存到静态变量里，从此全局可用。
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringContextUtil.applicationContext = applicationContext;
    }

    /**
     * 获取Spring Bean(根据类型)
     */
    public static <T> T getBean(Class<T> clazz) {return applicationContext.getBean(clazz);}

    /**
     * 获取Spring Bean(根据名称)
     */
    public static Object getBean(String name) {return applicationContext.getBean(name);}

    /**
     * 获取Spring Bean(根据类型和名称)
     */
    public static <T> T getBean(String name,Class<T> clazz) {return applicationContext.getBean(name,clazz);}
}
