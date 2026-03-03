package cn.edu.sxu.museai.core;


import cn.edu.sxu.museai.ai.model.HtmlCodeResult;
import cn.edu.sxu.museai.ai.model.MultiFileResult;
import cn.edu.sxu.museai.model.enums.CodeGenTypeEnum;
import cn.hutool.core.io.FileUtil;

import java.io.File;
import java.util.UUID;

/**
 * 文件路径 tmp/code/业务名/uuid/index.html
 */
public class CodeFileSaver {
    private static final String basePath = System.getProperty("user.dir") + "/tmp/code";

    public static File saveCode(HtmlCodeResult htmlCodeResult) {
        String htmlCode = htmlCodeResult.getHtmlCode();
        String fileDirPath = filePath(CodeGenTypeEnum.HTML);
        saveToFile(filePath(CodeGenTypeEnum.HTML), "index.html", htmlCode);
        return new File(fileDirPath);
    }

    public static File saveCode(MultiFileResult multiFileResult) {
        String fileDirPath = filePath(CodeGenTypeEnum.MULTI_FILE);
        saveToFile(fileDirPath, "index.html", multiFileResult.getHtmlCode());
        saveToFile(fileDirPath, "style.css", multiFileResult.getCssCode());
        saveToFile(fileDirPath, "script.js", multiFileResult.getJavaScriptCode());
        return new File(fileDirPath);
    }

    private static String filePath(CodeGenTypeEnum codeGenTypeEnum) {
        return basePath + "/" + codeGenTypeEnum.getValue() + "/" + UUID.randomUUID();
    }

    private static void saveToFile(String dirName, String fileName, String content) {
        FileUtil.writeUtf8String(content, dirName + "/" + fileName);
    }

}
