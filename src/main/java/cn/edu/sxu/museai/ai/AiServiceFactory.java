package cn.edu.sxu.museai.ai;

import cn.edu.sxu.museai.ai.tools.ToolsFactory;
import cn.edu.sxu.museai.model.enums.CodeGenTypeEnum;
import cn.edu.sxu.museai.service.HistoryService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class AiServiceFactory {
    private final ChatModel chatModel;
    private final StreamingChatModel streamingChatModel;
    private final ChatMemoryStore chatMemoryStore;

    @Resource
    private HistoryService historyService;

    private final Cache<Long, AiService> aiServiceCache = Caffeine
            .newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .removalListener((k,v,cause)->{
                log.info("AiService removed from cache: {}, cause: {}", k, cause);
            })
            .build();

    @Autowired
    public AiServiceFactory(ChatModel chatModel, StreamingChatModel streamingChatModel, ChatMemoryStore chatMemoryStore) {
        this.chatModel = chatModel;
        this.streamingChatModel = streamingChatModel;
        this.chatMemoryStore = chatMemoryStore;
    }

    public AiService aiService(Long appId) {
        return aiServiceCache.get(appId, k ->
            this.createAiService(appId, CodeGenTypeEnum.MULTI_FILE)
        );
    }
    public AiService aiService(Long appId, CodeGenTypeEnum codeGenTypeEnum) {
        return aiServiceCache.get(appId, k ->
            this.createAiService(appId, codeGenTypeEnum)
        );
    }


    public AiService createAiService(Long appId, CodeGenTypeEnum codeType) {
        String baseUrl = "https://open.bigmodel.cn/api/paas/v4/";
        String apiKey = System.getenv("GLM_KEY");
        String modelName = "GLM-4.7-FlashX";
        StreamingChatModel glmStreamingChatModel = OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .logRequests(true)
                .logResponses(true)
                .build();

        return switch (codeType) {
            case HTML, MULTI_FILE -> {
                MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                        .alwaysKeepSystemMessageFirst(true)
                        .id(appId)
                        .chatMemoryStore(chatMemoryStore)
                        .maxMessages(10)
                        .build();
                int n = historyService.loadMessageToMemory(chatMemoryStore, appId);
                log.info("Loaded {} messages to memory for app {}", n, appId);
                yield AiServices.builder(AiService.class)
                        .chatMemory(chatMemory)
                        .chatModel(chatModel)
                        .streamingChatModel(glmStreamingChatModel)
                        .build();
            }
            case VUE -> {
                MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                        .alwaysKeepSystemMessageFirst(true)
                        .id(appId)
                        .chatMemoryStore(chatMemoryStore)
                        .maxMessages(25)
                        .build();
                int n = historyService.loadMessageToMemory(chatMemoryStore, appId);
                log.info("Loaded {} messages to memory for app {}", n, appId);
                yield AiServices.builder(AiService.class)
                        .tools(ToolsFactory.tools())
                        .hallucinatedToolNameStrategy((request)-> {
                            log.info("大模型调用工具出现幻觉");
                            return ToolExecutionResultMessage.from(request, "该工具不存在");
                        })
                        .chatMemoryProvider((memoryId -> chatMemory))
                        .chatModel(chatModel)
                        .streamingChatModel(streamingChatModel)
                        .build();
            }
        };


    }

    @Bean
    public AiService createAiService(ChatModel chatModel, ChatMemoryStore chatMemoryStore) {
        String baseUrl = "https://open.bigmodel.cn/api/paas/v4/";
        String apiKey = System.getenv("GLM_KEY");
        String modelName = "GLM-4.7-FlashX";
        StreamingChatModel o = OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .logResponses(true)
                .logRequests(true)
                .build();

        return AiServices.builder(AiService.class)
                .chatModel(chatModel)
                .streamingChatModel(o)
                .chatMemoryProvider(memoryId ->
                    MessageWindowChatMemory.builder()
                            .chatMemoryStore(chatMemoryStore)
                            .id(memoryId)
                            .maxMessages(10)
                            .alwaysKeepSystemMessageFirst(true)
                            .build())
                .build();
    }
}
