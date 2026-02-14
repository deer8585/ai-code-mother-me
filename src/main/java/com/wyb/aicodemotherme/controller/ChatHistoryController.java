package com.wyb.aicodemotherme.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wyb.aicodemotherme.annotation.AuthCheck;
import com.wyb.aicodemotherme.common.BaseResponse;
import com.wyb.aicodemotherme.common.ResultUtils;
import com.wyb.aicodemotherme.constant.UserConstant;
import com.wyb.aicodemotherme.exception.ErrorCode;
import com.wyb.aicodemotherme.exception.ThrowUtils;
import com.wyb.aicodemotherme.model.dto.chathistory.ChatHistoryQueryRequest;
import com.wyb.aicodemotherme.model.entity.ChatHistory;
import com.wyb.aicodemotherme.model.entity.User;
import com.wyb.aicodemotherme.service.ChatHistoryService;
import com.wyb.aicodemotherme.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 对话历史 控制层。
 *
 * @author wyb
 */
@RestController
@RequestMapping("chatHistory")
public class ChatHistoryController {

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private UserService userService;

    /**
     * 分页查询某个应用的对话历史（游标查询）
     *
     * @param appId          应用ID
     * @param pageSize       页面大小
     * @param lastCreateTime 最后一条记录的创建时间
     * @param request        请求
     * @return 对话历史分页
     */
    @GetMapping("/app/{appId}")
    public BaseResponse<Page<ChatHistory>> listAppChatHistory(@PathVariable Long appId,
                                                              @RequestParam(defaultValue = "10") int pageSize,
                                                              @RequestParam(required = false) LocalDateTime lastCreateTime,
                                                              HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Page<ChatHistory> result = chatHistoryService.listAppChatHistoryByPage(appId, pageSize, lastCreateTime, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 管理员分页查询所有对话历史
     * 后台管理接口，只有管理员可以访问
     *
     * @param chatHistoryQueryRequest 查询请求参数，包含分页和筛选条件
     * @return 对话历史分页结果
     */
    @PostMapping("/admin/list/page/vo") //使用POST请求，适合传递复杂查询参数
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<ChatHistory>> listAppChatHistoryByPageForAdmin(
            //参数从请求体JSON中获取，支持复杂对象
            @RequestBody ChatHistoryQueryRequest chatHistoryQueryRequest) {
        ThrowUtils.throwIf(chatHistoryQueryRequest == null, ErrorCode.PARAMS_ERROR, "参数不能为空");

        Long pageNum = chatHistoryQueryRequest.getPageNum();
        Long pageSize = chatHistoryQueryRequest.getPageSize();

        QueryWrapper queryWrapper = chatHistoryService.getQueryWrapper(chatHistoryQueryRequest);

        Page<ChatHistory> result = chatHistoryService.page(Page.of(pageNum, pageSize), queryWrapper);
        return ResultUtils.success(result);
    }

}
