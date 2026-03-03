package cn.edu.sxu.museai.core.parser;

import cn.edu.sxu.museai.exception.ErrorCode;
import cn.edu.sxu.museai.exception.ThrowUtils;
import cn.edu.sxu.museai.model.enums.CodeGenTypeEnum;

public class CodeParserExecutor {

    private static final CodeParserStrategy htmlCodeParserStrategy = new HtmlParserStrategy();
    private static final CodeParserStrategy multiFileCodeParserStrategy = new MultiFileParserStrategy();

    public static Object parse(String code, CodeGenTypeEnum codeGenTypeEnum) {
        ThrowUtils.throwIf(codeGenTypeEnum == null,ErrorCode.PARAMS_ERROR,"codeGenTypeEnum is null");
        return switch (codeGenTypeEnum) {
            case HTML -> htmlCodeParserStrategy.parse(code);
            case MULTI_FILE -> multiFileCodeParserStrategy.parse(code);
        };
    }
}
