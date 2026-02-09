package com.wyb.aicodemotherme.common;

import lombok.Data;

import java.io.Serializable;

@Data
public class DeleteRequest implements Serializable { //实现序列化接口。
    //Serializable 是一个“标记接口”（内部没有方法）。
    // 它告诉 Java 虚拟机（JVM），这个类的对象可以被转换成字节流，从而进行网络传输或存储。

    /**
     * id
     */
    private Long id;

    //用于在序列化和反序列化过程中验证版本一致性。
    private static final long serialVersionUID = 1L; //定义序列化版本 ID

}
