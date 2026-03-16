package cn.edu.sxu.museai.graph.ai;


import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ImageCollectionPlanServiceFactory {

    @Bean
    public ImageCollectionPlanService imageCollectionPlanService() {
        String baseUrl = "https://open.bigmodel.cn/api/paas/v4/";
        String apiKey = System.getenv("GLM_KEY");
        String modelName = "GLM-4.7";

        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .logRequests(true)
                .logResponses(true)
                .apiKey(apiKey)
                .modelName(modelName)
                .build();

        return AiServices.builder(ImageCollectionPlanService.class)
                .chatModel(model)
                .build();
    }

}
