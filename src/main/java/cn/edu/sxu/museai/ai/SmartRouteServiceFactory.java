package cn.edu.sxu.museai.ai;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SmartRouteServiceFactory {

    @Bean
    public SmartRouteService getSmartRouteService() {

        String baseUrl = "https://open.bigmodel.cn/api/paas/v4/";
        String apiKey = System.getenv("GLM_KEY");
        String modelName = "GLM-4.7-FlashX";

        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .logRequests(true)
                .logResponses(true)
                .apiKey(apiKey)
                .modelName(modelName)
//                .maxRetries(1)
                .build();

        return AiServices.builder(SmartRouteService.class)
                .chatModel(model)
                .build();
    }

}
