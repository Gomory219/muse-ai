package cn.edu.sxu.museai.ai.model.message;

import dev.langchain4j.model.chat.response.PartialToolCall;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class ToolRequestMessage extends StreamMessage {

    private String toolName;
    private String toolId;
    private int toolIndex;

    public ToolRequestMessage(PartialToolCall partialToolCall) {
        this.type = StreamMessageTypeEnum.TOOL_REQUEST;
        this.toolName = partialToolCall.name();
        this.toolId = partialToolCall.id();
        this.toolIndex = partialToolCall.index();
    }
}
