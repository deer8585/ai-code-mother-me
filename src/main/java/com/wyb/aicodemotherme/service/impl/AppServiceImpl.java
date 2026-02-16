package com.wyb.aicodemotherme.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wyb.aicodemotherme.constant.AppConstant;
import com.wyb.aicodemotherme.core.AiCodeGeneratorFacade;
import com.wyb.aicodemotherme.core.builder.VueProjectBuilder;
import com.wyb.aicodemotherme.core.handler.StreamHandlerExecutor;
import com.wyb.aicodemotherme.exception.BusinessException;
import com.wyb.aicodemotherme.exception.ErrorCode;
import com.wyb.aicodemotherme.exception.ThrowUtils;
import com.wyb.aicodemotherme.model.dto.app.AppAddRequest;
import com.wyb.aicodemotherme.model.dto.app.AppQueryRequest;
import com.wyb.aicodemotherme.model.entity.App;
import com.wyb.aicodemotherme.model.entity.User;
import com.wyb.aicodemotherme.model.enums.ChatHistoryMessageTypeEnum;
import com.wyb.aicodemotherme.model.enums.CodeGenTypeEnum;
import com.wyb.aicodemotherme.model.vo.AppVO;
import com.wyb.aicodemotherme.model.vo.UserVO;
import com.wyb.aicodemotherme.service.AppService;
import com.wyb.aicodemotherme.mapper.AppMapper;
import com.wyb.aicodemotherme.service.ChatHistoryService;
import com.wyb.aicodemotherme.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author 28442
* @description 针对表【app(应用)】的数据库操作Service实现
* @createDate 2026-02-12 23:59:41
*/
@Service
@Slf4j
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService{

    @Resource
    private UserService userService;

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private StreamHandlerExecutor streamHandlerExecutor;

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    /**
     * 创建应用
     *
     * @param appAddRequest 创建应用请求
     * @param request       请求
     * @return 应用 id
     */
    @Override
    public Long createApp(AppAddRequest appAddRequest, HttpServletRequest request) {
        // 参数校验
        String initPrompt = appAddRequest.getInitPrompt();
        ThrowUtils.throwIf(StrUtil.isBlank(initPrompt), ErrorCode.PARAMS_ERROR, "初始化 prompt 不能为空");
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 构造入库对象
        App app = new App();
        BeanUtil.copyProperties(appAddRequest, app);
        app.setUserId(loginUser.getId());
        // 应用名称暂时为 initPrompt 前 12 位
        app.setAppName(initPrompt.substring(0, Math.min(initPrompt.length(), 12)));
        // todo 暂时设置为多文件生成
        app.setCodeGenType(CodeGenTypeEnum.VUE_PROJECT.getValue());
        // 插入数据库
        boolean result = this.save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return app.getId();
    }

    /**
     * 获取应用封装类
     * @param app
     * @return
     */
    @Override
    public AppVO getAppVO(App app) {
        if(app == null){
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);
        // 关联查询用户信息
        Long userId = app.getUserId();
        if(userId != null){
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            appVO.setUser(userVO);
        }
        return appVO;
    }

    /**
     * 构造QueryWrapper对象生成SQL查询
     * @param appQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<App> getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }

            Long id = appQueryRequest.getId();
            String appName = appQueryRequest.getAppName();
            String cover = appQueryRequest.getCover();
            String initPrompt = appQueryRequest.getInitPrompt();
            String codeGenType = appQueryRequest.getCodeGenType();
            String deployKey = appQueryRequest.getDeployKey();
            Integer priority = appQueryRequest.getPriority();
            Long userId = appQueryRequest.getUserId();
            String sortField = appQueryRequest.getSortField();
            String sortOrder = appQueryRequest.getSortOrder();

        QueryWrapper<App> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(codeGenType), "codeGenType", codeGenType);
        queryWrapper.eq(StrUtil.isNotBlank(deployKey), "deployKey", deployKey);
        queryWrapper.eq(ObjUtil.isNotNull(priority), "priority", priority);
        queryWrapper.eq(ObjUtil.isNotNull(userId), "userId", userId);

        //如果 appName 有值，则拼 AND appName LIKE '%xxx%'（MP 默认会自动加 %）
        queryWrapper.like(StrUtil.isNotBlank(appName), "appName", appName);
        queryWrapper.like(StrUtil.isNotBlank(cover), "cover", cover);
        queryWrapper.like(StrUtil.isNotBlank(initPrompt), "initPrompt", initPrompt);

        //StrUtil.isNotEmpty(sortField)：作为 condition
        //sortField 不为空才会排序，否则不拼 ORDER BY
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     * 获取应用封装列表
     * @param appList
     * @return
     */
    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        // 1. 批量收集所有需要查询的 userId
        // 获取appList中的userId集合,并去重
        Set<Long> userIds = appList.stream()
                .map(App::getUserId) // 提取每个 App 对象的 userId 属性
                .collect(Collectors.toSet()); // 收集到 Set 中自动去重

        // 2. 批量查询用户信息,避免 N+1 查询问题
        // 根据 userIds 批量查询用户信息，并转换为 Map 结构方便后续查找
        Map<Long, UserVO> userVOMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));

        // 3. 遍历 App 列表，转换为 AppVO 并填充关联的用户信息
        return appList.stream().map(app -> {
            AppVO appVO = getAppVO(app);
            UserVO userVO = userVOMap.get(app.getUserId());
            appVO.setUser(userVO);
            return appVO;
        }).collect(Collectors.toList());
    }

    /**
     * 通过对话生成应用代码
     * @param appId
     * @param message
     * @param loginUser
     * @return
     */
    @Override
    public Flux<String> chatToGenCode(Long appId, String message, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "用户消息不能为空");
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 验证用户是否有权限访问该应用，仅本人可以生成代码
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该应用");
        }
        // 4. 获取应用的代码生成类型
        String codeGenTypeStr = app.getCodeGenType();
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenTypeStr);
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型");
        }

        //5. 在调用AI前，先保存用户消息到数据库中
        chatHistoryService.addChatMessage(appId, message, ChatHistoryMessageTypeEnum.USER.getValue(), loginUser.getId());

        // 6. 调用 AI 生成代码
        Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream(message, codeGenTypeEnum, appId);

        //7. 收集AI响应内容并在完成之后记录到对话历史（根据不同类型进行收集）
        return streamHandlerExecutor.doExecute(codeStream,chatHistoryService,appId,loginUser,codeGenTypeEnum);
    }

    /**
     * 应用部署
     * @param appId 应用ID
     * @param loginUser 登录用户
     * @return 部署后的访问地址
     */
    @Override
    public String deployApp(Long appId, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 验证用户是否有权限部署该应用，仅本人可以部署
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限部署该应用");
        }
        // 4. 检查是否已有 deployKey
        String deployKey = app.getDeployKey();
        // 没有则生成 6 位 deployKey（大小写字母 + 数字）
        if (StrUtil.isBlank(deployKey)) {
            deployKey = RandomUtil.randomString(6);
        }
        // 5. 获取代码生成类型，构建源目录路径
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        // 6. 检查源目录是否存在
        File sourceDir = new File(sourceDirPath);
        //如果路径不存在，或者不是文件夹：说明没有生成代码/目录结构不对
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用代码不存在，请先生成代码");
        }

        // 7. Vue 项目特殊处理：执行构建
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if (codeGenTypeEnum == CodeGenTypeEnum.VUE_PROJECT) {
            // Vue 项目需要构建
            boolean buildSuccess = vueProjectBuilder.buildProject(sourceDirPath);
            ThrowUtils.throwIf(!buildSuccess, ErrorCode.SYSTEM_ERROR, "Vue 项目构建失败，请检查代码和依赖");
            // 检查 dist 目录是否存在
            File distDir = new File(sourceDirPath, "dist");
            ThrowUtils.throwIf(!distDir.exists(), ErrorCode.SYSTEM_ERROR, "Vue 项目构建完成但未生成 dist 目录");
            // 将 dist 目录作为部署源
            sourceDir = distDir;
            log.info("Vue 项目构建成功，将部署 dist 目录: {}", distDir.getAbsolutePath());
        }

        // 8. 复制文件到部署目录
        String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
        try {
            //把源目录内容复制到部署目录,true 表示覆盖复制
            FileUtil.copyContent(sourceDir, new File(deployDirPath), true);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "部署失败：" + e.getMessage());
        }
        // 9. 更新应用的 deployKey 和部署时间
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(LocalDateTime.now());
        boolean updateResult = this.updateById(updateApp);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新应用部署信息失败");
        // 10. 返回可访问的 URL
        return String.format("%s/%s/", AppConstant.CODE_DEPLOY_HOST, deployKey);
    }

    /**
     * 删除应用时关联删除对话历史
     *
     * @param id 应用ID
     * @return 是否成功
     */
    //重写myBatis-Plux的removeById方法
    @Override
    public boolean removeById(Serializable id) {
        if (id == null) {
            return false;
        }
        // 转换为 Long 类型
        Long appId = Long.valueOf(id.toString());
        if (appId <= 0) {
            return false;
        }
        // 先删除关联的对话历史
        try {
            chatHistoryService.deleteByAppId(appId);
        } catch (Exception e) {
            // 记录日志但不阻止应用删除
            log.error("删除应用关联对话历史失败: {}",e);
        }
        // 删除应用
        return super.removeById(id);
    }
}




