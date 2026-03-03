package cn.edu.sxu.museai.core.saver;

import cn.edu.sxu.museai.exception.ErrorCode;
import cn.edu.sxu.museai.exception.ThrowUtils;
import cn.edu.sxu.museai.model.enums.CodeGenTypeEnum;
import cn.hutool.core.io.FileUtil;

import java.io.File;
import java.util.UUID;

public abstract class CodeFileSaverTemplate<T> {
    private static final String basePath = System.getProperty("user.dir") + "/tmp/code";

    public File saveCode(T codeResult) {
        // 1. 校验输入
        validateInput(codeResult);
        // 2. 获取路径
        CodeGenTypeEnum codeGenTypeEnum = codeGenTypeEnum();
        String basePath = filePath(codeGenTypeEnum);
        // 3. 保存文件
        saveFiles(basePath,codeResult);
        return new File(basePath);
    }
    protected void validateInput(T input) {
        ThrowUtils.throwIf(input == null, ErrorCode.PARAMS_ERROR ,"input is null");
    }
    private String filePath(CodeGenTypeEnum codeGenTypeEnum) {
        String path = basePath + "/" + codeGenTypeEnum.getValue() + "/" + UUID.randomUUID();
        FileUtil.mkdir(path);
        return path;
    }
    protected final void saveToFile(String dirName, String fileName, String content) {
        String filePath = dirName + "/" + fileName;
        FileUtil.writeUtf8String(content, filePath);
    }
    protected abstract CodeGenTypeEnum codeGenTypeEnum();
    protected abstract void saveFiles(String basePath, T codeResult);

}
