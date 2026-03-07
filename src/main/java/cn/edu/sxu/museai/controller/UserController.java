package cn.edu.sxu.museai.controller;

import cn.edu.sxu.museai.aop.annotations.AuthCheck;
import cn.edu.sxu.museai.common.BaseResponse;
import cn.edu.sxu.museai.common.PageResult;
import cn.edu.sxu.museai.common.ResultUtils;
import cn.edu.sxu.museai.exception.ErrorCode;
import cn.edu.sxu.museai.exception.ThrowUtils;
import cn.edu.sxu.museai.model.dto.*;
import cn.edu.sxu.museai.model.dto.user.*;
import cn.edu.sxu.museai.model.enums.UserRoleEnum;
import cn.edu.sxu.museai.model.vo.LoginUserVO;
import cn.edu.sxu.museai.model.vo.UserVO;
import cn.hutool.core.bean.BeanUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import cn.edu.sxu.museai.model.entity.User;
import cn.edu.sxu.museai.service.UserService;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户 控制层。
 *
 * @author OneFish
 * @since 2026-02-28
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public Long register(@RequestBody UserRegisterRequest userRegisterRequest) {
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String confirmPassword = userRegisterRequest.getConfirmPassword();
        return userService.userRegister(userAccount, userPassword, confirmPassword);
    }
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(loginUserVO);
    }
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    @PostMapping("/save")
    @AuthCheck(UserRoleEnum.ADMIN)
    public BaseResponse<Long> save(UserAddRequest userAddRequest) {
        return ResultUtils.success(userService.saveUser(userAddRequest));
    }

    @AuthCheck(UserRoleEnum.ADMIN)
    @GetMapping("/get")
    public BaseResponse<User> get(Long id) {
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        return ResultUtils.success(user);
    }

    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getVO(Long id) {
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        UserVO userVO = userService.getUserVO(user);
        return ResultUtils.success(userVO);
    }
    @AuthCheck(UserRoleEnum.ADMIN)
    @GetMapping("/list")
    public BaseResponse<PageResult<UserVO>> list(UserQueryRequest userQueryRequest) {
        PageResult<UserVO> pageResult = userService.getUserVOList(userQueryRequest);
        return ResultUtils.success(pageResult);
    }

    @AuthCheck(UserRoleEnum.ADMIN)
    @PostMapping("/update")
    public BaseResponse<Boolean> update(@RequestBody UserUpdateRequest userUpdateRequest) {
        ThrowUtils.throwIf(userUpdateRequest.getId() == null, ErrorCode.PARAMS_ERROR);
        User user = BeanUtil.copyProperties(userUpdateRequest, User.class);
        boolean b = userService.updateById(user);
        return ResultUtils.success(b);
    }

    @AuthCheck(UserRoleEnum.ADMIN)
    @PostMapping("/delete")
    public BaseResponse<Boolean> delete(@RequestBody DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(deleteRequest.getId() == null, ErrorCode.PARAMS_ERROR);
        boolean b = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);
    }
}
