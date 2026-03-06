package cn.edu.sxu.museai.service.impl;

import cn.edu.sxu.museai.constant.UserConstant;
import cn.edu.sxu.museai.exception.ErrorCode;
import cn.edu.sxu.museai.exception.ThrowUtils;
import cn.edu.sxu.museai.model.dto.user.UserAddRequest;
import cn.edu.sxu.museai.model.dto.user.UserQueryRequest;
import cn.edu.sxu.museai.model.enums.UserRoleEnum;
import cn.edu.sxu.museai.model.vo.LoginUserVO;
import cn.edu.sxu.museai.model.vo.UserVO;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import cn.edu.sxu.museai.model.entity.User;
import cn.edu.sxu.museai.mapper.UserMapper;
import cn.edu.sxu.museai.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.List;

/**
 * 用户 服务层实现。
 *
 * @author OneFish
 * @since 2026-02-28
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        ThrowUtils.throwIf(StrUtil.hasBlank(userAccount, userPassword, checkPassword), ErrorCode.PARAMS_ERROR, "用户账户、密码、校验密码不能为空");
        ThrowUtils.throwIf(userAccount.length() < 4, ErrorCode.PARAMS_ERROR, "用户账户长度不能小于 4 位");
        ThrowUtils.throwIf(userPassword.length() < 6, ErrorCode.PARAMS_ERROR, "用户密码长度不能小于 6 位");
        ThrowUtils.throwIf(!userPassword.equals(checkPassword), ErrorCode.PARAMS_ERROR, "两次输入密码不一致");

        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq(User::getUserAccount, userAccount);
        User user = mapper.selectOneByQuery(queryWrapper);
        ThrowUtils.throwIf(user != null, ErrorCode.PARAMS_ERROR, "用户账户已存在");

        userPassword = encryptPassword(userPassword);
        user = User.builder()
                .userName("新用户" + userAccount)
                .userPassword(userPassword)
                .userAccount(userAccount)
                .userRole(UserRoleEnum.USER)
                .build();

        mapper.insertSelective(user);
        return user.getId();
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    @Override
    public UserVO getUserVO(User user) {
        return user == null ? null : BeanUtil.copyProperties(user, UserVO.class);
    }

    @Override
    public List<UserVO> getUserVOList(UserQueryRequest userQueryRequest) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        if (userQueryRequest.getId() != null) {
            queryWrapper.eq(User::getId, userQueryRequest.getId());
        }
        if (StrUtil.isNotBlank(userQueryRequest.getUserName())) {
            queryWrapper.like(User::getUserName, userQueryRequest.getUserName());
        }
        queryWrapper.like(User::getUserAccount, userQueryRequest.getUserAccount(), StrUtil::isNotBlank);
        queryWrapper.like(User::getUserProfile, userQueryRequest.getUserProfile(), StrUtil::isNotBlank);
        if (userQueryRequest.getUserRole() != null) {
            queryWrapper.eq(User::getUserRole, userQueryRequest.getUserRole());
        }
        if (StrUtil.isBlank(userQueryRequest.getSortOrder())) {
            queryWrapper.orderBy(userQueryRequest.getSortOrder(), !userQueryRequest.getSortOrder().equals("desc"));
        }
        QueryWrapper queryWrapper1 = new QueryWrapper();
        Page<User> paginate = mapper.paginate(userQueryRequest.toPage(), queryWrapper);
        List<User> users = paginate.getRecords();

        return users.stream().map(this::getUserVO).toList();
    }

    @Override
    public Long saveUser(UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(StrUtil.isBlank(userAddRequest.getUserName()), ErrorCode.PARAMS_ERROR, "用户账号不能为空");
        String defaultPassword = encryptPassword(UserConstant.DEFAULT_PASSWORD);
        User user = BeanUtil.copyProperties(userAddRequest, User.class);
        user.setUserPassword(defaultPassword);
        int effectedNums = mapper.insert(user);
        ThrowUtils.throwIf(effectedNums <= 0, ErrorCode.OPERATION_ERROR, "新增用户失败");
        return user.getId();
    }


    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        ThrowUtils.throwIf(StrUtil.hasBlank(userAccount, userPassword), ErrorCode.PARAMS_ERROR, "用户账户、密码不能为空");
        userPassword = encryptPassword(userPassword);
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq(User::getUserPassword, userPassword)
                .eq(User::getUserAccount, userAccount);
        User user = mapper.selectOneByQuery(queryWrapper);
        ThrowUtils.throwIf(user == null, ErrorCode.PARAMS_ERROR, "用户账户或密码错误");
        request.getSession().setAttribute(UserConstant.USER_INFO, user);
        return getLoginUserVO(user);
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        ThrowUtils.throwIf(request.getSession().getAttribute(UserConstant.USER_INFO) == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        request.getSession().removeAttribute(UserConstant.USER_INFO);
        return true;
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        User user = (User) request.getSession().getAttribute(UserConstant.USER_INFO);
        if (user == null) {
            return null;
        }
        user = mapper.selectOneByEntityId(user);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        request.setAttribute(UserConstant.USER_INFO, user);
        return user;
    }


    public String encryptPassword(String userPassword) {
        // 盐值，混淆密码
        final String SALT = "man!";
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }
}
