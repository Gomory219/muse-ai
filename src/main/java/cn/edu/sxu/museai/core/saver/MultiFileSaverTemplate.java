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
        if (codeResult.getHtmlCode() != null) {
            saveToFile(basePath, "index.html", codeResult.getHtmlCode());
        }
        if (codeResult.getCssCode() != null) {
            saveToFile(basePath, "style.css", codeResult.getCssCode());
        }
        if (codeResult.getJavaScriptCode() != null) {
            saveToFile(basePath, "script.js", codeResult.getJavaScriptCode());
        }
    }
}
