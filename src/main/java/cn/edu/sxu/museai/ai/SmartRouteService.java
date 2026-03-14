package cn.edu.sxu.museai.ai;

import cn.edu.sxu.museai.model.enums.CodeGenTypeEnum;

public interface SmartRouteService {
    CodeGenTypeEnum decide(String initPrompt);
}
