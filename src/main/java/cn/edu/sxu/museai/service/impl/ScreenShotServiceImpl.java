package cn.edu.sxu.museai.service.impl;

import cn.edu.sxu.museai.utils.MinioUtil;
import cn.edu.sxu.museai.service.ScreenShotService;
import cn.edu.sxu.museai.utils.WebScreenshotUtil;
import cn.hutool.core.io.FileUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ScreenShotServiceImpl implements ScreenShotService {

    @Resource
    private MinioUtil minioUtil;

    @Override
    public String takeScreenShot(String url) {

        String imagePath = WebScreenshotUtil.capture(url);
        if (imagePath == null) {
            log.error("Failed to capture screenshot for URL: {}", url);
            return null;
        }

        String minioUrl = minioUtil.uploadFile(imagePath);
        FileUtil.del(imagePath);
        return minioUrl;
    }
}
