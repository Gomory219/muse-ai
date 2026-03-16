package cn.edu.sxu.museai.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

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
                .build();
    }

}
