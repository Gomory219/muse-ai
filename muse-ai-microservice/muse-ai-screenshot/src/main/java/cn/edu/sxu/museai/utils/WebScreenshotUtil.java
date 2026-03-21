package cn.edu.sxu.museai.utils;


import cn.edu.sxu.museai.exception.BusinessException;
import cn.edu.sxu.museai.exception.ErrorCode;
import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.util.StrUtil;
import io.github.bonigarcia.wdm.WebDriverManager;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

@Slf4j
public class WebScreenshotUtil {
    /**
     *  WebDriver 访问浏览器的对象 在多线程下可能有并发安全问题
     */
    private static WebDriver webDriver;

    static {
        final int DEFAULT_WIDTH = 1600;
        final int DEFAULT_HEIGHT = 900;
        webDriver = initChromeDriver(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    private static void saveImage(byte[] imageBytes, String imagePath) {
        if (StrUtil.isBlank(imagePath)) {
            log.error("图片路径不能为空");
            return;
        }
        try {
            FileUtil.writeBytes(imageBytes, imagePath);
        } catch (IORuntimeException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存图片失败");
        }

    }

    private static void compressImage(String srcImagePath, String destImagePath) {
        if (!StrUtil.isAllNotEmpty(srcImagePath, destImagePath)) {
            log.error("图片路径不能为空");
            return;
        }
        try {
            final float DEFAULT_QUALITY = 0.8f;
            ImgUtil.compress(
                    new File(srcImagePath),
                    new File(destImagePath),
                    DEFAULT_QUALITY
            );
        } catch (IORuntimeException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "压缩图片失败");
        }
    }

    private static boolean waitForPageLoad(WebDriver webDriver) {
        try {
            WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(10));
            Boolean complete = wait.until(driver -> Objects.equals(
                    ((JavascriptExecutor) driver).executeScript("return document.readyState")
                    , "complete"));
            Thread.sleep(1000);
            return complete;
        } catch (Exception e) {
            log.error("等待页面加载完成失败", e);
            return false;
        }


    }

    public static String capture(String url) {
        if (StrUtil.isBlank(url)) {
            log.error("URL 不能为空");
            return null;
        }
        try {
            String rootPath = System.getProperty("user.dir") + "/tmp/screenshots";
            FileUtil.mkdir(rootPath);
            final String IMAGE_SUFFIX = ".png";
            String imagePath = rootPath + "/" + UUID.randomUUID() + IMAGE_SUFFIX;

            webDriver.get(url);
            if(!waitForPageLoad(webDriver)) {
                log.error("等待页面加载完成失败");
                return null;
            }
            byte[] imageBytes = ((TakesScreenshot) webDriver).getScreenshotAs(OutputType.BYTES);

            saveImage(imageBytes, imagePath);
            compressImage(imagePath, imagePath);

            log.info("截图成功，图片路径：{}", imagePath);
            return imagePath;
        } catch (WebDriverException e) {
            log.error("截图失败", e);
            return null;
        }

    }



    private static WebDriver initChromeDriver(int width, int height) {
        try {
            // 自动管理 ChromeDriver
            WebDriverManager.chromedriver().setup();
            // 配置 Chrome 选项
            ChromeOptions options = new ChromeOptions();
            // 无头模式
            options.addArguments("--headless");
            // 禁用GPU（在某些环境下避免问题）
            options.addArguments("--disable-gpu");
            // 禁用沙盒模式（Docker环境需要）
            options.addArguments("--no-sandbox");
            // 禁用开发者shm使用
            options.addArguments("--disable-dev-shm-usage");
            // 设置窗口大小
            options.addArguments(String.format("--window-size=%d,%d", width, height));
            // 禁用扩展
            options.addArguments("--disable-extensions");
            // 设置用户代理
            options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            // 创建驱动
            WebDriver driver = new ChromeDriver(options);
            // 设置页面加载超时
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            // 设置隐式等待
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            return driver;
        } catch (Exception e) {
            log.error("初始化 Chrome 浏览器失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "初始化 Chrome 浏览器失败");
        }
    }
    @PreDestroy
    private void destroy() {
        if (webDriver != null) {
            webDriver.quit();
            webDriver = null;
        }
    }
}

