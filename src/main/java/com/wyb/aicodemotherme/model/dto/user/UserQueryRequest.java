package com.wyb.aicodemotherme.model.dto.user;

import com.wyb.aicodemotherme.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 用户查询请求，需要继承公共包中的PageRequest来支持分页查询
 */
//callSuper = true: 明确告诉 Lombok：“在比较两个对象时，请务必把父类（PageRequest）里的字段也带上
//完整比较。只有当业务参数和分页参数都相同时，才认为相等。
@EqualsAndHashCode(callSuper = true)
@Data
public class UserQueryRequest extends PageRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin/ban
     */
    private String userRole;

    private static final long serialVersionUID = 1L;
}
