package cn.edu.sxu.museai.ai;

import cn.edu.sxu.museai.ai.tools.ToolsManager;
import cn.edu.sxu.museai.guardrail.PromptSafetyInputGuardrail;
import cn.edu.sxu.museai.model.enums.CodeGenTypeEnum;
import cn.edu.sxu.museai.service.HistoryService;
import cn.edu.sxu.museai.utils.SpringContextUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
    private ToolsManager toolsManager;
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
    public AiServiceFactory(@Qualifier("glmChatModelPrototype") ChatModel chatModel, @Qualifier("glmStreamingChatModelPrototype") StreamingChatModel streamingChatModel, ChatMemoryStore chatMemoryStore) {
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

        return switch (codeType) {
            case HTML, MULTI_FILE -> {
                StreamingChatModel glmStreamingChatModel = SpringContextUtil.getBean("glmStreamingChatModelPrototype", StreamingChatModel.class);
                ChatModel glmChatModel = SpringContextUtil.getBean("glmChatModelPrototype", ChatModel.class);
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
                        .chatModel(glmChatModel)
                        .streamingChatModel(glmStreamingChatModel)
                        .build();
            }
            case VUE -> {
                StreamingChatModel minimaxStreamingChatModel = SpringContextUtil.getBean("miniMaxStreamingChatModelPrototype", StreamingChatModel.class);
                ChatModel minimaxChatModel = SpringContextUtil.getBean("miniMaxChatModelPrototype", ChatModel.class);
                MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                        .alwaysKeepSystemMessageFirst(true)
                        .id(appId)
                        .chatMemoryStore(chatMemoryStore)
                        .maxMessages(40)
                        .build();
                int n = historyService.loadMessageToMemory(chatMemoryStore, appId);
                log.info("Loaded {} messages to memory for app {}", n, appId);
                yield AiServices.builder(AiService.class)
                        .tools((Object[]) toolsManager.allTools())
                        .hallucinatedToolNameStrategy((request)-> {
                            log.info("大模型调用工具出现幻觉");
                            return ToolExecutionResultMessage.from(request, "该工具不存在");
                        })
                        .chatMemoryProvider((memoryId -> chatMemory))
                        .chatModel(minimaxChatModel)
                        .inputGuardrails(new PromptSafetyInputGuardrail())
                        .streamingChatModel(minimaxStreamingChatModel)
                        .build();
            }
        };


    }

    @Bean
    public AiService createAiService(ChatMemoryStore chatMemoryStore) {
        return AiServices.builder(AiService.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
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
