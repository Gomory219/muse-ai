package cn.edu.sxu.museai.config;


import cn.edu.sxu.museai.monitor.AiModelMonitorListener;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import jakarta.annotation.Resource;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.List;

@Setter
@ConfigurationProperties(prefix = "glm")
@Configuration
public class GlmConfig {

    private String baseUrl;
    private String apiKey;
    private String modelName;
    private boolean logRequests;
    private boolean logResponses;
    private int maxTokens;

    @Resource
    private AiModelMonitorListener aiModelMonitorListener;


    @Bean
    @Scope("prototype")
    public StreamingChatModel glmStreamingChatModelPrototype() {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .maxTokens(maxTokens)
                .listeners(List.of(aiModelMonitorListener))
                .build();
    }

    @Bean
    @Scope("prototype")
    public ChatModel glmChatModelPrototype() {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .maxTokens(maxTokens)
                .listeners(List.of(aiModelMonitorListener))
                .build();
    }
}










