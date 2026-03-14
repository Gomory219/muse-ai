package cn.edu.sxu.museai.ai.tools;


import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class ToolsManager {

    @Resource
    private BaseTool[] tools;

    @Resource
    private Map<String, BaseTool> toolMap;


    @PostConstruct
    public void init() {
        for (BaseTool tool : tools) {
            toolMap.put(tool.getToolName(), tool);
        }
    }

    public BaseTool getTool(String toolName) {
        return toolMap.get(toolName);
    }

    public BaseTool[] allTools() {
        return tools;
    }

}
