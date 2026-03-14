package cn.edu.sxu.museai.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SmartRouteServiceFactory {

    @Bean
    public SmartRouteService getSmartRouteService() {

        String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
        String apiKey = System.getenv("ALIYUN_AI_KEY");
        String modelName = "qwen3.5-flash";


        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl(baseUrl)
//                .logRequests(true)
//                .logResponses(true)
                .apiKey(apiKey)
                .modelName(modelName)
//                .maxRetries(1)
                .build();

        return AiServices.builder(SmartRouteService.class)
                .chatModel(model)
                .build();
    }

}
