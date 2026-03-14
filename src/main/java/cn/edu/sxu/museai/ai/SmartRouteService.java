package cn.edu.sxu.museai.ai;

import cn.edu.sxu.museai.model.enums.CodeGenTypeEnum;
import dev.langchain4j.service.SystemMessage;

public interface SmartRouteService {

    @SystemMessage(fromResource = "prompt/SmartRoutePrompt.md")
    CodeGenTypeEnum decide(String initPrompt);
}
