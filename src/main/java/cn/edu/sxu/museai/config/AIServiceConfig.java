package cn.edu.sxu.museai.config;

import cn.edu.sxu.museai.ai.AiService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIServiceConfig {

    @Bean
    public AiService aiService(ChatModel chatModel) {
        return AiServices.create(AiService.class, chatModel);
    }

}
