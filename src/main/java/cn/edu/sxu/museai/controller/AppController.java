package cn.edu.sxu.museai.controller;

import cn.edu.sxu.museai.aop.annotations.AuthCheck;
import cn.edu.sxu.museai.common.BaseResponse;
import cn.edu.sxu.museai.common.ResultUtils;
import cn.edu.sxu.museai.model.dto.*;
import cn.edu.sxu.museai.model.dto.app.*;
import cn.edu.sxu.museai.model.entity.User;
import cn.edu.sxu.museai.model.enums.UserRoleEnum;
import cn.edu.sxu.museai.model.vo.AppVO;
import cn.edu.sxu.museai.service.AppService;
import cn.edu.sxu.museai.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 应用 控制层
 */
@RestController
@RequestMapping("/app")
@RequiredArgsConstructor
public class AppController {

    private final AppService appService;
    private final UserService userService;

    @PostMapping(value = "/chat",  produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatToGenApp(@RequestBody AppChatRequest appChatRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return appService.chatToGenApp(appChatRequest.getUserMessage(), appChatRequest.getAppId(), loginUser.getId());
    }

    @PostMapping("/deploy")
    public BaseResponse<String> deployApp(@RequestBody AppDeployRequest appDeployRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        String path = appService.deploy(appDeployRequest.getAppId() ,loginUser.getId());

        return null;
    }
    /**
     * 普通用户根据提示词创建应用
     * @param appAddRequest 应用创建请求
     * @param request http请求
     * @return 应用id
     */
    @PostMapping("/create")
    @AuthCheck(UserRoleEnum.USER)
    public BaseResponse<Long> createApp(@RequestBody AppAddRequest appAddRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Long appId = appService.createApp(appAddRequest, loginUser.getId());
        return ResultUtils.success(appId);
    }

    /**
     * 普通用户修改应用名称
     * @param appNameUpdateRequest 应用名称更新请求
     * @param request http请求
     * @return 是否成功
     */
    @AuthCheck(UserRoleEnum.USER)
    @PostMapping("/update/name")
    public BaseResponse<Boolean> updateAppName(@RequestBody AppNameUpdateRequest appNameUpdateRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Boolean result = appService.updateAppName(appNameUpdateRequest, loginUser.getId());
        return ResultUtils.success(result);
    }

    /**
     * 普通用户删除自己的应用
     * @param id 应用id
     * @param request http请求
     * @return 是否成功
     */
    @AuthCheck(UserRoleEnum.USER)
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteApp(@RequestParam("id") Long id, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Boolean result = appService.deleteApp(id, loginUser.getId());
        return ResultUtils.success(result);
    }

    /**
     * 普通用户查看自己的应用详情
     * @param id 应用id
     * @param request http请求
     * @return 应用详情
     */
    @AuthCheck(UserRoleEnum.USER)
    @GetMapping("/get")
    public BaseResponse<AppVO> getAppDetail(@RequestParam("id") Long id, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        AppVO appVO = appService.getAppDetail(id, loginUser.getId());
        return ResultUtils.success(appVO);
    }

    /**
     * 普通用户分页查询自己的应用列表
     * @param appQueryRequest 查询请求
     * @param request http请求
     * @return 应用列表
     */
    @AuthCheck(UserRoleEnum.USER)
    @GetMapping("/list/my")
    public BaseResponse<List<AppVO>> listMyApps(AppQueryRequest appQueryRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        List<AppVO> appList = appService.listUserApps(appQueryRequest, loginUser.getId());
        return ResultUtils.success(appList);
    }

    /**
     * 查询精选应用列表，按优先级排序
     * @param appQueryRequest 查询请求
     * @return 应用列表
     */
    @GetMapping("/list/featured")
    public BaseResponse<List<AppVO>> listFeaturedApps(AppQueryRequest appQueryRequest) {
        List<AppVO> appList = appService.listFeaturedApps(appQueryRequest);
        return ResultUtils.success(appList);
    }

    /**
     * 管理员删除任意应用
     * @param deleteRequest 应用id
     * @return 是否成功
     */
    @AuthCheck(UserRoleEnum.ADMIN)
    @PostMapping("/admin/delete")
    public BaseResponse<Boolean> deleteAppByAdmin(@RequestBody DeleteRequest deleteRequest) {
        Boolean result = appService.deleteAppByAdmin(deleteRequest);
        return ResultUtils.success(result);
    }

    /**
     * 管理员更新应用信息
     * @param appUpdateRequest 应用更新请求
     * @return 是否成功
     */
    @AuthCheck(UserRoleEnum.ADMIN)
    @PostMapping("/admin/update")
    public BaseResponse<Boolean> updateAppByAdmin(@RequestBody AppUpdateRequest appUpdateRequest) {
        Boolean result = appService.updateAppByAdmin(appUpdateRequest);
        return ResultUtils.success(result);
    }

    /**
     * 管理员分页查询应用列表
     * @param appQueryRequest 查询请求
     * @return 应用列表
     */
    @AuthCheck(UserRoleEnum.ADMIN)
    @GetMapping("/admin/list")
    public BaseResponse<List<AppVO>> listAppsByAdmin(AppQueryRequest appQueryRequest) {
        List<AppVO> appList = appService.listAppsByAdmin(appQueryRequest);
        return ResultUtils.success(appList);
    }

    /**
     * 管理员查看应用详情
     * @param id 应用id
     * @return 应用详情
     */
    @AuthCheck(UserRoleEnum.ADMIN)
    @GetMapping("/admin/get")
    public BaseResponse<AppVO> getAppDetailByAdmin(@RequestParam("id") Long id) {
        AppVO appVO = appService.getAppDetailByAdmin(id);
        return ResultUtils.success(appVO);
    }
}
