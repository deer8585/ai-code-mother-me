package com.wyb.aicodemotherme.controller;

import com.wyb.aicodemotherme.common.BaseResponse;
import com.wyb.aicodemotherme.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class healthController {

    @GetMapping()
    public BaseResponse<String> healthController() {
        return ResultUtils.success("OK");
    }
}
