package com.wyb.aicodemotherme.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wyb.aicodemotherme.annotation.AuthCheck;
import com.wyb.aicodemotherme.common.BaseResponse;
import com.wyb.aicodemotherme.common.DeleteRequest;
import com.wyb.aicodemotherme.common.ResultUtils;
import com.wyb.aicodemotherme.constant.AppConstant;
import com.wyb.aicodemotherme.constant.UserConstant;
import com.wyb.aicodemotherme.exception.BusinessException;
import com.wyb.aicodemotherme.exception.ErrorCode;
import com.wyb.aicodemotherme.exception.ThrowUtils;
import com.wyb.aicodemotherme.model.dto.app.*;
import com.wyb.aicodemotherme.model.entity.App;
import com.wyb.aicodemotherme.model.entity.User;
import com.wyb.aicodemotherme.model.enums.CodeGenTypeEnum;
import com.wyb.aicodemotherme.model.vo.AppVO;
import com.wyb.aicodemotherme.service.AppService;
import com.wyb.aicodemotherme.service.ProjectDownloadService;
import com.wyb.aicodemotherme.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/app")
@Slf4j
public class AppController {

    @Resource
    private AppService appService;

    @Resource
    private UserService userService;

    @Resource
    private ProjectDownloadService projectDownloadService;

    /**
     * 创建应用
     *
     * @param appAddRequest 创建应用请求
     * @param request       请求
     * @return 应用 id
     */
    @PostMapping("/add")
    public BaseResponse<Long> addApp(@RequestBody AppAddRequest appAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appAddRequest == null, ErrorCode.PARAMS_ERROR);
        Long appId = appService.createApp(appAddRequest,request);
        return ResultUtils.success(appId);
    }

    /**
     * 更新应用（用户只能更新自己的应用名称）
     *
     * @param appUpdateRequest 更新请求
     * @param request          请求
     * @return 更新结果
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateApp(@RequestBody AppUpdateRequest appUpdateRequest, HttpServletRequest request) {
        if (appUpdateRequest == null || appUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long id = appUpdateRequest.getId();
        // 判断是否存在
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);

        // 仅本人可更新
        if (!oldApp.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        App app = new App();
        app.setId(id);
        app.setAppName(appUpdateRequest.getAppName());

        // 设置编辑时间
        app.setEditTime(LocalDateTime.now());
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 删除应用（用户只能删除自己的应用）
     *
     * @param deleteRequest 删除请求
     * @param request       请求
     * @return 删除结果
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteApp(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldApp.getUserId().equals(loginUser.getId()) && !UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = appService.removeById(id);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取应用详情
     *
     * @param id      应用 id
     * @return 应用详情
     */
    @GetMapping("/get/vo")
    public BaseResponse<AppVO> getAppVOById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类（包含用户信息）
        return ResultUtils.success(appService.getAppVO(app));
    }

    /**
     * 分页获取当前用户创建的应用列表
     *
     * @param appQueryRequest 查询请求
     * @param request         请求
     * @return 应用列表
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<AppVO>> listMyAppVOByPage(@RequestBody AppQueryRequest appQueryRequest, HttpServletRequest request) {
        //1. 参数校验
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);

        //2. 获取当前登录用户
        User loginUser = userService.getLoginUser(request);

        //3. 分页参数校验
        long pageSize = appQueryRequest.getPageSize();
        // 校验每页大小，限制最多20条，防止恶意请求大量数据
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR, "每页最多查询 20 个应用");
        // 校验页码
        long pageNum = appQueryRequest.getPageNum();
        // 4. 设置查询条件：只查询当前用户的应用
        // 将登录用户的 ID 设置到查询条件中，实现数据权限控制
        appQueryRequest.setUserId(loginUser.getId());

        // 5. 构建查询条件
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);

        // 6. 分页查询
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);

        // 7. 数据转换和封装
        // 创建新的 Page<AppVO> 对象，继承原始分页信息（页码、每页大小、总记录数）
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotal());

        // 将 App 实体列表转换为 AppVO 列表（包含关联的用户信息）
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());

        // 将转换后的 VO 列表设置到分页对象中
        appVOPage.setRecords(appVOList);

        // 8. 返回结果
        return ResultUtils.success(appVOPage);
    }

    /**
     * 分页获取精选应用列表
     *
     * @param appQueryRequest 查询请求
     * @return 精选应用列表
     */
    @PostMapping("/good/list/page/vo")
    public BaseResponse<Page<AppVO>> listGoodAppVOByPage(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 限制每页最多 20 个
        long pageSize = appQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR, "每页最多查询 20 个应用");
        long pageNum = appQueryRequest.getPageNum();
        // 只查询精选的应用
        appQueryRequest.setPriority(AppConstant.GOOD_APP_PRIORITY);
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        // 分页查询
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);
        // 数据封装
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotal());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }

    /**
     * 管理员删除应用
     *
     * @param deleteRequest 删除请求
     * @return 删除结果
     */
    @PostMapping("/admin/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteAppByAdmin(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        // 判断是否存在
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = appService.removeById(id);
        return ResultUtils.success(result);
    }

    /**
     * 管理员更新应用
     *
     * @param appAdminUpdateRequest 更新请求
     * @return 更新结果
     */
    @PostMapping("/admin/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateAppByAdmin(@RequestBody AppAdminUpdateRequest appAdminUpdateRequest) {
        if (appAdminUpdateRequest == null || appAdminUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = appAdminUpdateRequest.getId();
        // 判断是否存在
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        App app = new App();
        BeanUtil.copyProperties(appAdminUpdateRequest, app);
        // 设置编辑时间
        app.setEditTime(LocalDateTime.now());
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 管理员分页获取应用列表
     *
     * @param appQueryRequest 查询请求
     * @return 应用列表
     */
    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<AppVO>> listAppVOByPageByAdmin(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = appQueryRequest.getPageNum();
        long pageSize = appQueryRequest.getPageSize();
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);
        // 数据封装
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotal());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }

    /**
     * 管理员根据 id 获取应用详情
     *
     * @param id 应用 id
     * @return 应用详情
     */
    @GetMapping("/admin/get/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<AppVO> getAppVOByIdByAdmin(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(appService.getAppVO(app));
    }

    /**
     * 应用聊天生成代码（流式 SSE返回）
     *
     * @param appId   应用 ID
     * @param message 用户消息
     * @param request 请求对象
     * @return 生成结果流
     */
    //声明为SSE流式返回，使用get请求便于前端使用EventSource对接
    //produces = MediaType.TEXT_EVENT_STREAM_VALUE：告诉 Spring 返回内容类型是 text/event-stream
    //也就是 SSE 必需的 content-type，不然 EventSource 接不起来。
    @GetMapping(value = "/chat/gen/code",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    //返回值：Flux<ServerSentEvent<String>>
    //Flux：Reactor 响应式流，代表多条数据不断发出（流式）
    //ServerSentEvent<String>：每一条 SSE 事件（可以设置 event 类型、data 内容等）
    public Flux<ServerSentEvent<String>> chatToGenCode(@RequestParam Long appId,
                                                       @RequestParam String message,
                                                       HttpServletRequest request) {
        try {
            //1. 参数校验
            ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
            ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "用户消息不能为空");
            //2. 获取当前登录用户
            User loginUser = userService.getLoginUser(request);
            // 3.调用服务层方法，获取原始内容流（返回Flux<String>）
            Flux<String> contentFlux = appService.chatToGenCode(appId, message, loginUser);

            // 4. 将原始内容流转换为 ServerSentEvent 格式
            return contentFlux
                    .map(chunk -> {
                        //Map.of("d", chunk)：把 chunk 放进一个 Map，key 是 "d"
                        Map<String, String> wrapper = Map.of("d", chunk);
                        //将Map转换为JSON字符串：{"d":"这里是一段生成的代码..."}
                        //为什么要包 JSON？
                        //因为 SSE 的 data 本质是字符串，JSON 能扩展更多字段（比如以后加 type、id、lang 等）
                        String jsonData = JSONUtil.toJsonStr(wrapper);
                        // 构建ServerSentEvent对象
                        return ServerSentEvent.<String>builder()
                                //设置事件数据内容
                                .data(jsonData) //设置 SSE 的 data 字段（最终会变成 SSE 协议里的 data: ...）
                                .build();
                    })
                    // 处理错误
                    .onErrorResume(throwable -> {
                        log.error("代码生成过程中发生错误", throwable);
                        // 发送错误事件
                        Map<String, String> errorWrapper = Map.of("error", throwable.getMessage());
                        String errorData = JSONUtil.toJsonStr(errorWrapper);
                        //Mono.just(...)：只发 一条 SSE 事件
                        return Mono.just(ServerSentEvent.<String>builder()
                                .event("error") //事件类型标记为 "error"
                                .data(errorData) //data 是 {"error":"xxx"}
                                .build());
                    })
                    // 5. 发送结束事件
                    .concatWith(
                            //使用Mono创建单个结束事件
                            Mono.just(
                                    //构建结束标志事件
                                    ServerSentEvent.<String>builder()
                                            //设置事件类型为done
                                            .event("done") //告诉前端“生成结束”
                                            //设置事件数据为空
                                            .data("")
                                            .build()
                            )
                    );
        } catch (Exception e) { //如果参数校验、获取用户、调用 service 初始化阶段就抛异常，会进入这里
            log.error("参数校验或初始化失败", e);
            // 对于同步异常（还没进入 Flux 就出错），返回错误事件流
            Map<String, String> errorWrapper = Map.of("error", e.getMessage());
            String errorData = JSONUtil.toJsonStr(errorWrapper);
            //Flux.just(a, b)：一次性发两条 SSE：
            //event=error，告诉前端错误原因
            //event=done，告诉前端结束（避免前端一直等）
            return Flux.just(
                    ServerSentEvent.<String>builder()
                            .event("error")
                            .data(errorData)
                            .build(),
                    ServerSentEvent.<String>builder()
                            .event("done")
                            .data("")
                            .build()
            );
        }
    }

    /**
     * 应用部署
     *
     * @param appDeployRequest 部署请求
     * @param request          请求
     * @return 部署 URL
     */
    @PostMapping("/deploy")
    public BaseResponse<String> deployApp(@RequestBody AppDeployRequest appDeployRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appDeployRequest == null, ErrorCode.PARAMS_ERROR);
        Long appId = appDeployRequest.getAppId();
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 调用服务部署应用
        String deployUrl = appService.deployApp(appId, loginUser);
        return ResultUtils.success(deployUrl);
    }

    /**
     * 下载应用代码
     *
     * @param appId    应用ID
     * @param request  请求
     * @param response 响应
     */
    @GetMapping("/download/{appId}")
    public void downloadAppCode(@PathVariable Long appId,
                                HttpServletRequest request,
                                HttpServletResponse response) {
        // 1. 基础校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        // 2. 查询应用信息
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 权限校验：只有应用创建者可以下载代码
        User loginUser = userService.getLoginUser(request);
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限下载该应用代码");
        }
        // 4. 构建应用代码目录路径（生成目录，非部署目录）
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        // 5. 检查代码目录是否存在
        File sourceDir = new File(sourceDirPath);
        ThrowUtils.throwIf(!sourceDir.exists() || !sourceDir.isDirectory(),
                ErrorCode.NOT_FOUND_ERROR, "应用代码不存在，请先生成代码");
        // 6. 生成下载文件名（不建议添加中文内容）
        String downloadFileName = String.valueOf(appId);
        // 7. 调用通用下载服务
        projectDownloadService.downloadProjectAsZip(sourceDirPath, downloadFileName, response);
    }

}
