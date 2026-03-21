package cn.edu.sxu.museai.ai.model.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ResponseStreamMessage extends StreamMessage {
    private String response;

    public ResponseStreamMessage(String chunk) {
        this.response = chunk;
        this.type = StreamMessageTypeEnum.AI_RESPONSE;
    }
}
