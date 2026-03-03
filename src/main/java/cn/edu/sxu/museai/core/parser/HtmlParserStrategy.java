package cn.edu.sxu.museai.core.parser;

import cn.edu.sxu.museai.ai.model.HtmlCodeResult;

public class HtmlParserStrategy implements CodeParserStrategy<HtmlCodeResult> {
    @Override
    public HtmlCodeResult parse(String code) {
        return AiCodeResponseParser.parseSingleFile(code);
    }
}
