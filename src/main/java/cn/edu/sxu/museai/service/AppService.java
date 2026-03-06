package cn.edu.sxu.museai.service;

import cn.edu.sxu.museai.model.dto.*;
import cn.edu.sxu.museai.model.dto.app.AppAddRequest;
import cn.edu.sxu.museai.model.dto.app.AppNameUpdateRequest;
import cn.edu.sxu.museai.model.dto.app.AppQueryRequest;
import cn.edu.sxu.museai.model.dto.app.AppUpdateRequest;
import cn.edu.sxu.museai.model.vo.UserVO;
import com.mybatisflex.core.service.IService;
import cn.edu.sxu.museai.model.entity.App;
import cn.edu.sxu.museai.model.vo.AppVO;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 应用 服务层
 */
public interface AppService extends IService<App> {

    Flux<String> chatToGenApp(String userMessage, Long appId, Long userId);

    /**
     * 创建应用
     *
     * @param appAddRequest 创建应用请求
     * @param userId        创建用户id
     * @return 应用id
     */
    Long createApp(AppAddRequest appAddRequest, Long userId);

    /**
     * 更新应用名称（普通用户）
     *
     * @param appNameUpdateRequest 更新应用名称请求
     * @param userId               当前用户id
     * @return 是否成功
     */
    Boolean updateAppName(AppNameUpdateRequest appNameUpdateRequest, Long userId);

    /**
     * 删除应用（普通用户，只能删除自己的）
     *
     * @param id     应用id
     * @param userId 当前用户id
     * @return 是否成功
     */
    Boolean deleteApp(Long id, Long userId);

    /**
     * 获取应用详情（普通用户，只能查看自己的）
     *
     * @param id     应用id
     * @param userId 当前用户id
     * @return 应用视图对象
     */
    AppVO getAppDetail(Long id, Long userId);

    /**
     * 分页查询用户的应用列表
     *
     * @param appQueryRequest 查询请求
     * @param userId          当前用户id
     * @return 应用视图对象列表
     */
    List<AppVO> listUserApps(AppQueryRequest appQueryRequest, Long userId);

    /**
     * 查询精选应用列表（按优先级排序）
     *
     * @param appQueryRequest 查询请求（仅支持appName模糊查询）
     * @return 应用视图对象列表
     */
    List<AppVO> listFeaturedApps(AppQueryRequest appQueryRequest);

    /**
     * 管理员删除任意应用
     *
     * @param id 应用id
     * @return 是否成功
     */
    Boolean deleteAppByAdmin(DeleteRequest id);

    /**
     * 管理员更新应用
     *
     * @param appUpdateRequest 更新请求
     * @return 是否成功
     */
    Boolean updateAppByAdmin(AppUpdateRequest appUpdateRequest);

    /**
     * 管理员分页查询应用列表
     *
     * @param appQueryRequest 查询请求
     * @return 应用视图对象列表
     */
    List<AppVO> listAppsByAdmin(AppQueryRequest appQueryRequest);

    /**
     * 管理员获取应用详情
     *
     * @param id 应用id
     * @return 应用视图对象
     */
    AppVO getAppDetailByAdmin(Long id);

    /**
     * 转换为视图对象
     *
     * @param app 应用实体
     * @return 应用视图对象
     */
    AppVO getAppVO(App app);

    String deploy(String appId, Long userId);
}
