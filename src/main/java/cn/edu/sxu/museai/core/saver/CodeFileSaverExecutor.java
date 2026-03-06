package cn.edu.sxu.museai.core.saver;

import cn.edu.sxu.museai.ai.model.HtmlCodeResult;
import cn.edu.sxu.museai.ai.model.MultiFileResult;
import cn.edu.sxu.museai.exception.ErrorCode;
import cn.edu.sxu.museai.exception.ThrowUtils;
import cn.edu.sxu.museai.model.enums.CodeGenTypeEnum;

import java.io.File;

public class CodeFileSaverExecutor {

    private static final CodeFileSaverTemplate<HtmlCodeResult> htmlCodeFileSaverTemplate = new HtmlCodeFileSaverTemplate();
    private static final CodeFileSaverTemplate<MultiFileResult> multiFileSaverTemplate = new MultiFileSaverTemplate();

    public static File saveFile(Object codeResult, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        ThrowUtils.throwIf(codeGenTypeEnum == null, ErrorCode.PARAMS_ERROR, "codeGenTypeEnum is null");
        return switch (codeGenTypeEnum) {
            case HTML -> htmlCodeFileSaverTemplate.saveCode((HtmlCodeResult) codeResult, appId);
            case MULTI_FILE -> multiFileSaverTemplate.saveCode((MultiFileResult) codeResult, appId);
        };
    }
}
