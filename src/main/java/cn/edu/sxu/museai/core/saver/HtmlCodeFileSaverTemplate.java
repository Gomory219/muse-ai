package cn.edu.sxu.museai.core.saver;

import cn.edu.sxu.museai.ai.model.HtmlCodeResult;
import cn.edu.sxu.museai.model.enums.CodeGenTypeEnum;
import cn.hutool.core.io.FileUtil;

public class HtmlCodeFileSaverTemplate extends CodeFileSaverTemplate<HtmlCodeResult> {
    @Override
    protected CodeGenTypeEnum codeGenTypeEnum() {
        return CodeGenTypeEnum.HTML;
    }

    @Override
    protected void saveFiles(String basePath, HtmlCodeResult codeResult) {
        saveToFile(basePath, "index.html", codeResult.getHtmlCode());
    }

}
