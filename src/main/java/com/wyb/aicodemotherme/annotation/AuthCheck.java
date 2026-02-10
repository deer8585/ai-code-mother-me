package com.wyb.aicodemotherme.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD) //元注解。规定这个注解只能标记在方法上（不能写在类、变量或包上）。
//元注解。规定该注解在运行时依然有效。这样程序在运行期间，可以通过“反射”机制读取到这个注解的信息，从而进行逻辑判断。
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthCheck { //定义一个名为 AuthCheck 的注解。@interface 是定义注解的关键字。

    /**
     * 必须有某个角色
     */
    //定义注解的一个成员属性，名为 mustRole。它的默认值是空字符串。使用时可以写成 @AuthCheck(mustRole = "ADMIN")。
    String mustRole() default "";
}
