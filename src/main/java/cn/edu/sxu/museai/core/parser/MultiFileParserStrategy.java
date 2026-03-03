package cn.edu.sxu.museai.core.parser;

import cn.edu.sxu.museai.ai.model.MultiFileResult;

public class MultiFileParserStrategy implements CodeParserStrategy<MultiFileResult> {
    @Override
    public MultiFileResult parse(String code) {
        return AiCodeResponseParser.parseMultiFile(code);
    }
}
