package cn.edu.sxu.museai.core.builder;

import cn.hutool.core.util.RuntimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class VueProjectBuilder {

    public void buildProjectAsync(String projectPath) {
        Thread.ofVirtual().name("vue-project-builder").start(() -> {
            try {
                if (buildProject(projectPath)) {
                    log.info("Vue project built successfully: {}", projectPath);
                } else {
                    log.error("Vue project build failed: {}", projectPath);
                }
            } catch (Exception e) {
                log.error("Error building Vue project: {}", projectPath, e);
            }
        });
    }

    public boolean buildProject(String projectPath) {
        File projectDir = new File(projectPath);
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            log.error("项目目录不存在: {}", projectPath);
            return false;
        }
        // 检查 package.json 是否存在
        File packageJson = new File(projectDir, "package.json");
        if (!packageJson.exists()) {
            log.error("package.json 文件不存在: {}", packageJson.getAbsolutePath());
            return false;
        }
        log.info("开始构建 Vue 项目: {}", projectPath);
        // 执行 npm install
        if (!npmInstall(projectDir)) {
            log.error("npm install 执行失败");
            return false;
        }
        // 执行 npm run build
        if (!npmRunBuild(projectDir)) {
            log.error("npm run build 执行失败");
            return false;
        }
        // 验证 dist 目录是否生成
        File distDir = new File(projectDir, "dist");
        if (!distDir.exists()) {
            log.error("构建完成但 dist 目录未生成: {}", distDir.getAbsolutePath());
            return false;
        }
        log.info("Vue 项目构建成功，dist 目录: {}", distDir.getAbsolutePath());
        return true;
    }

    private boolean npmInstall(File dir) {
        return executedCommand(dir, "npm install", 120);
    }

    private boolean npmRunBuild(File dir) {
        return executedCommand(dir, "npm run build", 120);
    }

    private boolean executedCommand(File directory, String command, int timeout) {
        try {
            if (isWindows()) {
                command = "cmd /c " + command;
            }
            Process process = RuntimeUtil.exec(null, directory, command);
            boolean result = process.waitFor(timeout, TimeUnit.SECONDS);
            if (!result) {
                process.destroyForcibly();
                log.error("Command execution timed out");
                return false;
            }
            int exitValue = process.exitValue();
            if (exitValue != 0) {
                log.error("Command execution failed with exit value: {}", exitValue);
                return false;
            }
            return true;
        } catch (InterruptedException e) {
            log.error("Command execution interrupted", e);
            return false;
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

}
