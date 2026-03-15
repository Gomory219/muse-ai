package cn.edu.sxu.museai.ai;

import cn.edu.sxu.museai.graph.tools.ImageSearchTool;
import cn.edu.sxu.museai.graph.tools.LogoGeneratorTool;
import cn.edu.sxu.museai.graph.tools.MermaidDiagramTool;
import cn.edu.sxu.museai.graph.tools.UndrawIllustrationTool;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeneralAiServiceServiceFactory {

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

    @Bean
    public ImageCollectionService getImageCollectionService(
            ImageSearchTool imageSearchTool,
            LogoGeneratorTool logoGeneratorTool,
            MermaidDiagramTool mermaidDiagramTool,
            UndrawIllustrationTool undrawIllustrationTool
//            ChatModel model
    ) {
        String baseUrl = "https://open.bigmodel.cn/api/paas/v4/";
        String apiKey = System.getenv("GLM_KEY");
        String modelName = "GLM-4.7";

        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .logRequests(true)
                .logResponses(true)
                .apiKey(apiKey)
//                .supportedCapabilities(Capability.RESPONSE_FORMAT_JSON_SCHEMA)
                .modelName(modelName)
//                .strictJsonSchema(true)
//                .maxRetries(1)
                .build();

        return AiServices.builder(ImageCollectionService.class)
                .chatModel(model)
                .tools(
                    imageSearchTool,
                    logoGeneratorTool,
                    mermaidDiagramTool,
                    undrawIllustrationTool
                )
                .build();
    }

}
