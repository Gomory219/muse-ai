package cn.edu.sxu.museai.core.saver;

import cn.edu.sxu.museai.ai.model.MultiFileResult;
import cn.edu.sxu.museai.model.enums.CodeGenTypeEnum;

public class MultiFileSaverTemplate extends CodeFileSaverTemplate<MultiFileResult> {
    @Override
    protected CodeGenTypeEnum codeGenTypeEnum() {
        return CodeGenTypeEnum.MULTI_FILE;
    }

    @Override
    protected void saveFiles(String basePath, MultiFileResult codeResult) {
        saveToFile(basePath, "index.html", codeResult.getHtmlCode());
        saveToFile(basePath, "style.css", codeResult.getCssCode());
        saveToFile(basePath, "script.js", codeResult.getJavaScriptCode());
    }
}
