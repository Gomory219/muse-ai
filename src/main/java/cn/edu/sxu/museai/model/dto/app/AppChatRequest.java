package cn.edu.sxu.museai.model.dto.app;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppChatRequest {
    private String userMessage;
    private Long appId;
}
