package cn.edu.sxu.museai.service.impl;

import cn.edu.sxu.museai.constant.AppConstant;
import cn.edu.sxu.museai.core.AiCodeGeneratorFacade;
import cn.edu.sxu.museai.exception.BusinessException;
import cn.edu.sxu.museai.model.dto.app.AppAddRequest;
import cn.edu.sxu.museai.model.dto.app.AppNameUpdateRequest;
import cn.edu.sxu.museai.model.dto.app.AppQueryRequest;
import cn.edu.sxu.museai.model.dto.app.AppUpdateRequest;
import cn.edu.sxu.museai.model.entity.App;
import cn.edu.sxu.museai.exception.ErrorCode;
import cn.edu.sxu.museai.exception.ThrowUtils;
import cn.edu.sxu.museai.mapper.AppMapper;
import cn.edu.sxu.museai.mapper.UserMapper;
import cn.edu.sxu.museai.model.dto.*;
import cn.edu.sxu.museai.model.entity.User;
import cn.edu.sxu.museai.model.enums.CodeGenTypeEnum;
import cn.edu.sxu.museai.model.vo.AppVO;
import cn.edu.sxu.museai.model.vo.UserVO;
import cn.edu.sxu.museai.service.AppService;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 应用 服务层实现
 */
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    @Resource
    private UserMapper userMapper;
    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Override
    public Flux<String> chatToGenApp(String userMessage, Long appId, Long userId) {
        ThrowUtils.throwIf(StrUtil.isBlank(userMessage), ErrorCode.PARAMS_ERROR, "用户消息不能为空");
        ThrowUtils.throwIf(appId == null, ErrorCode.PARAMS_ERROR, "应用id不能为空");
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.eq(App::getId, appId);
        App app = mapper.selectOneByQuery(queryWrapper);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        ThrowUtils.throwIf(!app.getUserId().equals(userId), ErrorCode.NO_AUTH_ERROR, "无权访问此应用");

        Flux<String> stringFlux = aiCodeGeneratorFacade.generateCodeAndSaveStreaming(userMessage, CodeGenTypeEnum.MULTI_FILE, appId);

        Map<String, String> e = Map.of("e", "end");
        return stringFlux.map(chunk -> {
            Map<String, String> result = Map.of("v", chunk);
            return JSONUtil.toJsonStr(result);
        }).concatWithValues(JSONUtil.toJsonStr(e));
    }

    @Override
    public String deploy(String appId, Long userId) {
        ThrowUtils.throwIf(StrUtil.isBlank(appId), ErrorCode.PARAMS_ERROR, "应用id不能为空");
        QueryWrapper qw = QueryWrapper.create().eq(App::getId, appId);
        App app = getOne(qw);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        ThrowUtils.throwIf(!app.getUserId().equals(userId), ErrorCode.NO_AUTH_ERROR, "无权部署此应用");

        String deployKey = app.getDeployKey();
        if (deployKey == null) {
            deployKey = RandomUtil.randomString(6);
        }
        String sourcePath = AppConstant.CODE_BATH_PATH + "/" + app.getCodeGenType().getValue() + "/" + appId;
        File sourceDir = new File(sourcePath);
        ThrowUtils.throwIf(!sourceDir.exists(), ErrorCode.NOT_FOUND_ERROR, "应用代码不存在");
        String targetPath = AppConstant.APP_BATH_PATH + "/" + RandomUtil.randomString(6) + deployKey;
        try {
            FileUtil.copy(sourcePath, targetPath, true);
        } catch (IORuntimeException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "部署失败" + e.getMessage());
        }

        app.setDeployKey(deployKey);
        app.setDeployedTime(LocalDateTime.now());
        boolean b = updateById(app);
        ThrowUtils.throwIf(!b, ErrorCode.OPERATION_ERROR, "部署失败");
        return AppConstant.CODE_DEPLOY_HOST + "/" + deployKey;
    }

    @Override
    public Long createApp(AppAddRequest appAddRequest, Long userId) {
        ThrowUtils.throwIf(appAddRequest == null, ErrorCode.PARAMS_ERROR, "创建请求不能为空");
        String initPrompt = appAddRequest.getInitPrompt();
        ThrowUtils.throwIf(StrUtil.isBlank(initPrompt), ErrorCode.PARAMS_ERROR, "初始化prompt不能为空");

        App app = App.builder()
                .initPrompt(initPrompt)
                .appName(initPrompt.substring(0, Integer.min(12, initPrompt.length())))
                .codeGenType(CodeGenTypeEnum.HTML)
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
        app.setEditTime(LocalDateTime.now());
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
    public Boolean deleteAppByAdmin(DeleteRequest deleteRequest) {
        Long id = deleteRequest.getId();
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
        AppVO appVO = BeanUtil.copyProperties(app, AppVO.class);
        // 关联查询用户信息
        if (app.getUserId() != null) {
            User queryUser = new User();
            queryUser.setId(app.getUserId());
            User user = userMapper.selectOneByEntityId(queryUser);
            if (user != null) {
                UserVO userVO = BeanUtil.copyProperties(user, UserVO.class);
                appVO.setUser(userVO);
            }
        }
        return appVO;
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
