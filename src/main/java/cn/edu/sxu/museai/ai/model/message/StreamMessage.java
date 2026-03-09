package cn.edu.sxu.museai.ai.model.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StreamMessage {
    protected StreamMessageTypeEnum type;
}
