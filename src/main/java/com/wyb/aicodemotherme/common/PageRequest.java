package com.wyb.aicodemotherme.common;

import lombok.Data;

@Data
public class PageRequest {

    /**
     * 当前页号
     */
    private Long pageNum = 1L;

    /**
     * 页面大小
     */
    private Long pageSize = 10L;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序顺序（默认降序）
     */
    private String sortOrder = "descend";
}
