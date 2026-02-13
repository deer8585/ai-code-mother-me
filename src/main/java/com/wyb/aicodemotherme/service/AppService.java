package com.wyb.aicodemotherme.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wyb.aicodemotherme.model.dto.app.AppAddRequest;
import com.wyb.aicodemotherme.model.dto.app.AppQueryRequest;
import com.wyb.aicodemotherme.model.entity.App;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wyb.aicodemotherme.model.entity.User;
import com.wyb.aicodemotherme.model.vo.AppVO;
import jakarta.servlet.http.HttpServletRequest;
import reactor.core.publisher.Flux;

import java.util.List;

/**
* @author 28442
* @description 针对表【app(应用)】的数据库操作Service
* @createDate 2026-02-12 23:59:41
*/
public interface AppService extends IService<App> {

    /**
     * 创建应用
     *
     * @param appAddRequest 创建应用请求
     * @param request       请求
     * @return 应用 id
     */
    Long createApp(AppAddRequest appAddRequest, HttpServletRequest request);

    /**
     * 获取应用封装类
     * @param app
     * @return
     */
    AppVO getAppVO(App app);

    /**
     * 构造QueryWrapper对象生成SQL查询
     * @param appQueryRequest
     * @return
     */
    QueryWrapper<App> getQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     * 获取应用封装列表
     * @param appList
     * @return
     */
    List<AppVO> getAppVOList(List<App> appList);

    /**
     * 通过对话生成应用代码
     * @param appId
     * @param message
     * @param loginUser
     * @return
     */
    Flux<String> chatToGenCode(Long appId, String message, User loginUser);

    /**
     * 应用部署
     * @param appId 应用ID
     * @param loginUser 登录用户
     * @return 部署后的访问地址
     */
    String deployApp(Long appId, User loginUser);
}
