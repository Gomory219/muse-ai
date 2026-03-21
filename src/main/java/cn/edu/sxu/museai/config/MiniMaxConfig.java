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

@Configuration
@ConfigurationProperties(prefix = "minimax")
@Setter
public class MiniMaxConfig {

    private String baseUrl;
    private String apiKey;
    private String modelName;
    private boolean logRequest;
    private boolean logResponse;
    private int maxTokens;

    @Resource
    private AiModelMonitorListener aiModelMonitorListener;

    @Bean
    @Scope("prototype")
    public ChatModel miniMaxChatModelPrototype() {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .apiKey(apiKey)
                .logRequests(logRequest)
                .logResponses(logResponse)
                .maxTokens(maxTokens)
                .listeners(List.of(aiModelMonitorListener))
                .build();
    }

    @Bean
    @Scope("prototype")
    public StreamingChatModel miniMaxStreamingChatModelPrototype() {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .apiKey(apiKey)
                .logRequests(logRequest)
                .logResponses(logResponse)
                .maxTokens(maxTokens)
                .listeners(List.of(aiModelMonitorListener))
                .build();
    }

}
