package cn.edu.sxu.museai.config;

import cn.edu.sxu.museai.constant.AppConstant;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置外部目录，Windows 路径需要 file: 前缀
        String codePath = "file:" + AppConstant.CODE_BATH_PATH;
        String downloadPath = "file:" + AppConstant.CODE_DOWNLOAD_PATH;
        registry.addResourceHandler("/code/**")
                .addResourceLocations(codePath);
        registry.addResourceHandler("/download/**")
                .addResourceLocations(downloadPath);
    }
}