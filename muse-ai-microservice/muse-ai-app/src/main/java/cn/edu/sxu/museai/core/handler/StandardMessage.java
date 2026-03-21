package cn.edu.sxu.museai.core.handler;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StandardMessage {
    /**
     * 消息类型
     */
    private JsonViewType jsonViewType;
    /**
     * 消息内容
     */
    private String v;
    /**
     * 工具名称
     */
    private String toolName;

    /**
     * 工具执行结果
     */
    private String toolResult;

    private Boolean success;

    public static StandardMessage finishMessage() {
        return StandardMessage.builder()
                .jsonViewType(JsonViewType.FINISH)
                .build();
    }
}
