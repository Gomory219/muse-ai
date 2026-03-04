package cn.edu.sxu.museai.service.impl;

import cn.edu.sxu.museai.constant.AppConstant;
import cn.edu.sxu.museai.entity.App;
import cn.edu.sxu.museai.exception.ErrorCode;
import cn.edu.sxu.museai.exception.ThrowUtils;
import cn.edu.sxu.museai.mapper.AppMapper;
import cn.edu.sxu.museai.model.dto.AppAddRequest;
import cn.edu.sxu.museai.model.dto.AppNameUpdateRequest;
import cn.edu.sxu.museai.model.dto.AppQueryRequest;
import cn.edu.sxu.museai.model.dto.AppUpdateRequest;
import cn.edu.sxu.museai.model.vo.AppVO;
import cn.edu.sxu.museai.service.AppService;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 应用 服务层实现
 */
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    @Override
    public Long createApp(AppAddRequest appAddRequest, Long userId) {
        ThrowUtils.throwIf(appAddRequest == null, ErrorCode.PARAMS_ERROR, "创建请求不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(appAddRequest.getInitPrompt()), ErrorCode.PARAMS_ERROR, "初始化prompt不能为空");

        App app = App.builder()
                .initPrompt(appAddRequest.getInitPrompt())
                .userId(userId)
                .priority(AppConstant.DEFAULT_PRIORITY)
                .build();

        boolean saved = save(app);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "创建应用失败");
        return app.getId();
    }

    @Override
    public Boolean updateAppName(AppNameUpdateRequest appNameUpdateRequest, Long userId) {
        ThrowUtils.throwIf(appNameUpdateRequest == null, ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        ThrowUtils.throwIf(appNameUpdateRequest.getId() == null, ErrorCode.PARAMS_ERROR, "应用id不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(appNameUpdateRequest.getAppName()), ErrorCode.PARAMS_ERROR, "应用名称不能为空");

        // 查询应用并验证所有权
        App app = getById(appNameUpdateRequest.getId());
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        ThrowUtils.throwIf(!app.getUserId().equals(userId), ErrorCode.NO_AUTH_ERROR, "无权修改此应用");

        // 更新应用名称
        app.setAppName(appNameUpdateRequest.getAppName());
        return updateById(app);
    }

    @Override
    public Boolean deleteApp(Long id, Long userId) {
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR, "应用id不能为空");

        // 查询应用并验证所有权
        App app = getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        ThrowUtils.throwIf(!app.getUserId().equals(userId), ErrorCode.NO_AUTH_ERROR, "无权删除此应用");

        return removeById(id);
    }

    @Override
    public AppVO getAppDetail(Long id, Long userId) {
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR, "应用id不能为空");

        App app = getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        ThrowUtils.throwIf(!app.getUserId().equals(userId), ErrorCode.NO_AUTH_ERROR, "无权查看此应用");

        return getAppVO(app);
    }

    @Override
    public List<AppVO> listUserApps(AppQueryRequest appQueryRequest, Long userId) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(userId == null, ErrorCode.PARAMS_ERROR, "用户id不能为空");

        QueryWrapper queryWrapper = buildQueryWrapper(appQueryRequest);
        // 强制只能查询当前用户的应用
        queryWrapper.eq(App::getUserId, userId);

        Page<App> page = page(appQueryRequest.toPage(), queryWrapper);
        return page.getRecords().stream()
                .map(this::getAppVO)
                .toList();
    }

    @Override
    public List<AppVO> listFeaturedApps(AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);

        QueryWrapper queryWrapper = QueryWrapper.create();

        // 只支持应用名称模糊查询
        if (StrUtil.isNotBlank(appQueryRequest.getAppName())) {
            queryWrapper.like(App::getAppName, appQueryRequest.getAppName());
        }

        // 按优先级降序排序
        queryWrapper.orderBy(App::getPriority, false)
                .orderBy(App::getCreateTime, false);

        Page<App> page = page(appQueryRequest.toPage(), queryWrapper);
        return page.getRecords().stream()
                .map(this::getAppVO)
                .toList();
    }

    @Override
    public Boolean deleteAppByAdmin(Long id) {
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR, "应用id不能为空");

        App app = getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");

        return removeById(id);
    }

    @Override
    public Boolean updateAppByAdmin(AppUpdateRequest appUpdateRequest) {
        ThrowUtils.throwIf(appUpdateRequest == null, ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        ThrowUtils.throwIf(appUpdateRequest.getId() == null, ErrorCode.PARAMS_ERROR, "应用id不能为空");

        // 验证应用存在
        App existApp = getById(appUpdateRequest.getId());
        ThrowUtils.throwIf(existApp == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");

        // 更新字段
        App app = new App();
        app.setId(appUpdateRequest.getId());
        if (StrUtil.isNotBlank(appUpdateRequest.getAppName())) {
            app.setAppName(appUpdateRequest.getAppName());
        }
        if (StrUtil.isNotBlank(appUpdateRequest.getCover())) {
            app.setCover(appUpdateRequest.getCover());
        }
        if (appUpdateRequest.getPriority() != null) {
            app.setPriority(appUpdateRequest.getPriority());
        }

        return updateById(app);
    }

    @Override
    public List<AppVO> listAppsByAdmin(AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);

        QueryWrapper queryWrapper = buildQueryWrapper(appQueryRequest);

        Page<App> page = page(appQueryRequest.toPage(), queryWrapper);
        return page.getRecords().stream()
                .map(this::getAppVO)
                .toList();
    }

    @Override
    public AppVO getAppDetailByAdmin(Long id) {
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR, "应用id不能为空");

        App app = getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");

        return getAppVO(app);
    }

    @Override
    public AppVO getAppVO(App app) {
        if (app == null) {
            return null;
        }
        return BeanUtil.copyProperties(app, AppVO.class);
    }

    /**
     * 构建查询条件
     *
     * @param appQueryRequest 查询请求
     * @return 查询包装器
     */
    private QueryWrapper buildQueryWrapper(AppQueryRequest appQueryRequest) {
        QueryWrapper queryWrapper = QueryWrapper.create();

        if (appQueryRequest.getId() != null) {
            queryWrapper.eq(App::getId, appQueryRequest.getId());
        }
        if (StrUtil.isNotBlank(appQueryRequest.getAppName())) {
            queryWrapper.like(App::getAppName, appQueryRequest.getAppName());
        }
        if (appQueryRequest.getUserId() != null) {
            queryWrapper.eq(App::getUserId, appQueryRequest.getUserId());
        }
        if (StrUtil.isNotBlank(appQueryRequest.getCodeGenType())) {
            queryWrapper.eq(App::getCodeGenType, appQueryRequest.getCodeGenType());
        }
        if (appQueryRequest.getMinPriority() != null) {
            queryWrapper.ge(App::getPriority, appQueryRequest.getMinPriority());
        }
        if (appQueryRequest.getMaxPriority() != null) {
            queryWrapper.le(App::getPriority, appQueryRequest.getMaxPriority());
        }

        // 排序
        if (StrUtil.isNotBlank(appQueryRequest.getSortField())) {
            boolean isAsc = "ascend".equalsIgnoreCase(appQueryRequest.getSortOrder());
            queryWrapper.orderBy(appQueryRequest.getSortField(), isAsc);
        } else {
            // 默认按创建时间降序
            queryWrapper.orderBy(App::getCreateTime, false);
        }

        return queryWrapper;
    }
}
