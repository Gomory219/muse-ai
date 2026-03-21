package cn.edu.sxu.museai.aop;

import cn.edu.sxu.museai.aop.annotations.AuthCheck;
import cn.edu.sxu.museai.common.ResultUtils;
import cn.edu.sxu.museai.constant.UserConstant;
import cn.edu.sxu.museai.enums.UserRoleEnum;
import cn.edu.sxu.museai.exception.ErrorCode;
import cn.edu.sxu.museai.model.entity.User;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class AuthAspect {

    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint proceedingJoinPoint, AuthCheck authCheck) throws Throwable {
        UserRoleEnum requiredRole = authCheck.value();
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        for (Cookie cookie : request.getCookies()) {
            System.out.println(cookie.getName());
            System.out.println(cookie.getValue());
        }
        User user = (User) request.getSession().getAttribute(UserConstant.USER_INFO);
        if(user == null || user.getUserRole() == null) {
            return ResultUtils.error(ErrorCode.NOT_LOGIN_ERROR,"未登录");
        }
        if(requiredRole == UserRoleEnum.ADMIN && user.getUserRole() != requiredRole) {
            return ResultUtils.error(ErrorCode.NO_AUTH_ERROR,"权限不足");
        }
        return proceedingJoinPoint.proceed();
    }
}
