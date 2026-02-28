package cn.edu.sxu.museai.interceptor;

import cn.edu.sxu.museai.common.ResultUtils;
import cn.edu.sxu.museai.constant.UserConstant;
import cn.edu.sxu.museai.model.entity.User;
import cn.edu.sxu.museai.model.enums.UserRoleEnum;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class UserInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        User user = (User) request.getSession().getAttribute(UserConstant.USER_INFO);
        if(user == null) {
            ResultUtils.handleUnLogin(response);
            return false;
        } else if(!user.getUserRole().equals(UserRoleEnum.ADMIN)) {
            ResultUtils.handleUnauthorized(response);
            return false;
        }
        return true;
    }
}
