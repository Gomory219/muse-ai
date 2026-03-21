package cn.edu.sxu.museai.service.impl;

import cn.edu.sxu.museai.client.innerservice.InnerScreenShotService;
import cn.edu.sxu.museai.service.ScreenShotService;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService
public class InnerScreenShotServiceImpl implements InnerScreenShotService {
    @Resource
    private ScreenShotService screenShotService;

    @Override
    public String generateAndUploadScreenshot(String url) {
        return screenShotService.takeScreenShot(url);
    }
}
