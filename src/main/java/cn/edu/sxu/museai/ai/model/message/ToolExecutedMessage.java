package cn.edu.sxu.museai.ai.model.message;


import cn.hutool.json.JSONUtil;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.tool.ToolExecution;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class ToolExecutedMessage extends StreamMessage {
    private String toolName;
    private String toolOutput;
    private String arguments;
    private String toolId;

    public ToolExecutedMessage(ToolExecution toolExecution) {
        ToolExecutionRequest request = toolExecution.request();
        this.type = StreamMessageTypeEnum.TOOL_EXECUTED;
        this.toolName = request.name();
        this.toolOutput = toolExecution.result();
        this.arguments = request.arguments();
        this.toolId = request.id();
    }
}

