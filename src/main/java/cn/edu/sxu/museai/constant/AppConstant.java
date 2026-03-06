package cn.edu.sxu.museai.constant;

/**
 * 应用常量
 */
public interface AppConstant {

    /**
     * 默认优先级
     */
    Integer DEFAULT_PRIORITY = 0;

    /**
     * 代码文件保存路径
     */
    String CODE_BATH_PATH = System.getProperty("user.dir") + "/tmp/code";

    /**
     * 部署文件保存路径
     */
    String APP_BATH_PATH = System.getProperty("user.dir") + "/tmp/app";

    /**
     * 部署域名
     */
    String CODE_DEPLOY_HOST = "http://localhost";
}
