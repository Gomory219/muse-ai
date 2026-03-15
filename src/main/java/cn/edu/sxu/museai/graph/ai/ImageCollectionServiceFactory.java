package cn.edu.sxu.museai.graph.ai;

import cn.edu.sxu.museai.graph.tools.ImageSearchTool;
import cn.edu.sxu.museai.graph.tools.LogoGeneratorTool;
import cn.edu.sxu.museai.graph.tools.MermaidDiagramTool;
import cn.edu.sxu.museai.graph.tools.UndrawIllustrationTool;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class ImageCollectionServiceFactory {
    @Bean
    public ImageCollectionService imageCollectionService(
            ImageSearchTool imageSearchTool,
            LogoGeneratorTool logoGeneratorTool,
            MermaidDiagramTool mermaidDiagramTool,
            UndrawIllustrationTool undrawIllustrationTool
    ) {
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
