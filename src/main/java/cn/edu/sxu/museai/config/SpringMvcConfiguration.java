package cn.edu.sxu.museai.config;

import cn.edu.sxu.museai.interceptor.AdminInterceptor;
import cn.edu.sxu.museai.interceptor.UserInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class SpringMvcConfiguration implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
//        registry.addInterceptor(new UserInterceptor()).addPathPatterns("/**");
//        registry.addInterceptor(new AdminInterceptor()).addPathPatterns("/admin/**");
    }
}
