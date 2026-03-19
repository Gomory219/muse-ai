package cn.edu.sxu.museai.ratelimit;


import cn.edu.sxu.museai.exception.BusinessException;
import cn.edu.sxu.museai.exception.ErrorCode;
import cn.edu.sxu.museai.exception.ThrowUtils;
import cn.edu.sxu.museai.model.entity.User;
import cn.edu.sxu.museai.service.UserService;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

@Aspect
@Component
@Slf4j
@Order(-1)
public class RateLimitAspect {

    @Resource
    private RedissonClient redissonClient;
    @Resource
    private UserService userService;

    @Before("@annotation(rateLimit)")
    public void doRateLimit(JoinPoint joinPoint, RateLimit rateLimit) {

        String key = generateKey(joinPoint, rateLimit);
        if (StrUtil.isBlank(key)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        int rate = rateLimit.rate();
        int i = rateLimit.rateInterval();
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        rateLimiter.expire(Duration.ofSeconds(i));

        rateLimiter.trySetRate(RateType.OVERALL, rate, Duration.ofSeconds(i));

        ThrowUtils.throwIf(!rateLimiter.tryAcquire(1), ErrorCode.TOO_MANY_REQUEST);
    }

    private String generateKey(JoinPoint joinPoint, RateLimit rateLimit) {
        RateLimitType rateLimitType = rateLimit.limitType();
        StringBuilder sb = new StringBuilder();
        sb.append("rate:limit:").append(rateLimit.limitType().name()).append(":").append(rateLimit.key()).append(":");
        switch (rateLimitType) {
            case API: {
                MethodSignature signature = (MethodSignature)joinPoint.getSignature();
                String className = signature.getMethod().getDeclaringClass().getSimpleName();
                String methodName = signature.getMethod().getName();
                sb.append(className).append(":").append(methodName);
                return sb.toString();
            }
            case USER: {
                ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (requestAttributes == null) {
                    return null;
                }
                HttpServletRequest request = requestAttributes.getRequest();
                User user = userService.getLoginUser(request);
                sb.append(user.getId());
                return sb.toString();
            }

            case IP: {
                sb.append(getClientIP());
                return sb.toString();
            }
            default:
                return null;
        }
    }

    private String getClientIP() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }
        HttpServletRequest request = attributes.getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多级代理的情况
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "unknown";
    }

}
