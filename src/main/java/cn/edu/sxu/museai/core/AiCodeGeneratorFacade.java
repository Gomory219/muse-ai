package cn.edu.sxu.museai.core;

import cn.edu.sxu.museai.ai.AiService;
import cn.edu.sxu.museai.ai.AiServiceFactory;
import cn.edu.sxu.museai.ai.model.message.ResponseStreamMessage;
import cn.edu.sxu.museai.ai.model.message.ToolExecutedMessage;
import cn.edu.sxu.museai.ai.model.message.ToolRequestMessage;
import cn.edu.sxu.museai.core.handler.MessageHandlerExecutor;
import cn.edu.sxu.museai.core.parser.CodeParserExecutor;
import cn.edu.sxu.museai.core.saver.CodeFileSaverExecutor;
import cn.edu.sxu.museai.exception.ErrorCode;
import cn.edu.sxu.museai.exception.ThrowUtils;
import cn.edu.sxu.museai.model.enums.CodeGenTypeEnum;
import cn.hutool.json.JSONUtil;
import dev.langchain4j.service.TokenStream;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.File;

@Slf4j
@Component
public class AiCodeGeneratorFacade {
    @Resource
    private AiService aiService;
    @Resource
    private AiServiceFactory aiServiceFactory;
    @Resource
    private MessageHandlerExecutor messageHandlerExecutor;
    /**
     *  根据用户消息和代码生成类型生成代码并保存为文件
     * @param userMessage 用户输入的消息，用于生成代码
     * @param codeGenTypeEnum 代码生成类型枚举，决定生成单文件还是多文件
     * @return 生成的代码文件对象
     */
    public File generateCodeAndSave(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        ThrowUtils.throwIf(codeGenTypeEnum == null, ErrorCode.PARAMS_ERROR, "请选择代码生成模式");
        Object codeResult = switch (codeGenTypeEnum) {
            case HTML -> aiService.generateSingleFile(userMessage);
            case MULTI_FILE -> aiService.generateMultiFile(userMessage);
            case VUE -> null;
        };
        return CodeFileSaverExecutor.saveFile(codeResult, codeGenTypeEnum, appId);
    }

    /**
     * 根据用户信息和生成类型生成文件并保存为文件（流式）
     * 具体来说做了以下内容
     * 1. 根据参数的提示词和代码生成类型，生成代码
     * 2. 根据输入的代码生成类型进行后续的流处理：
     *  - 如果是单文件生成类型或多文件生成类型，则监听流并在生成结束时将代码保存至文件
     * 3. 将输出的流按照标准格式化为json，并返回给前端
     * 4. 在输出结束时，将对话信息保存到数据库中
     * @param userMessage 用户输入的消息，用于生成代码
     * @param codeGenTypeEnum 代码生成类型枚举，决定生成单文件还是多文件
     * @return AI输出流
     */
    public Flux<String> generateCodeAndSaveStreaming(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId, Long userId) {
        ThrowUtils.throwIf(codeGenTypeEnum == null, ErrorCode.PARAMS_ERROR, "请选择代码生成模式");
        AiService aiServiceWithMemory = aiServiceFactory.aiService(appId, codeGenTypeEnum);
        /*
         * 根据枚举生成代码流并进行初步处理：
         *  - 如果是单文件生成类型或多文件生成类型，则监听流并在生成结束时将代码保存至文件
         *  - 如果是Vue生成类型，则将 TokenStream流转化为Flux<String>流，并将 AI 的各种输出标准化为 StreamMessage 的Json
         */
        Flux<String> stringFlux = switch (codeGenTypeEnum) {
            case HTML -> aiServiceWithMemory.generateSingleFileStreaming(userMessage);

            case MULTI_FILE -> aiServiceWithMemory.generateMultiFileStreaming(userMessage);

            case VUE ->{
                TokenStream tokenStream = aiServiceWithMemory.generateVueProjectStreaming(userMessage, appId);
                yield processTokenStream(tokenStream);
            }
        };
        return messageHandlerExecutor.execute(stringFlux, appId, codeGenTypeEnum, userId);
    }

    /**
     * 处理 TokenStream 流，将 AI 的各种输出标准化为 StreamMessage 的Json
     * 为什么需要用 TokenStream ？ 因为将AiService的返回值定义为Flux<String>，则这个流中只会有 Ai响应中的 Response 部分，
     * 即 data.choices[0].delta.content，而这部分内容显然不能满足前端展示需求（要展示工具调用细节，包括选择工具调用和工具调用结果）
     * 因此需要使用 TokenStream 来处理 AiService 的返回值，以便获取完整的 AI 输出内容
     * 而这个方法将 TokenStream 流转化为 Flux<String> 流，并将 AI 的各种输出标准化为 StreamMessage 的Json
     * @param tokenStream TokenStream 流
     * @return 处理后的流
     */
    private Flux<String> processTokenStream(TokenStream tokenStream){
        return Flux.create(sink -> {
            tokenStream.onPartialResponse((chunk) -> {
                        ResponseStreamMessage responseStreamMessage = new ResponseStreamMessage(chunk);
                        sink.next(JSONUtil.toJsonStr(responseStreamMessage));
                    })
                    .onPartialToolCall((partialToolCall)->{
                        ToolRequestMessage toolRequestMessage = new ToolRequestMessage(partialToolCall);
                        sink.next(JSONUtil.toJsonStr(toolRequestMessage));
                    })
                    .onToolExecuted((toolCall)->{
                        ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolCall);
                        sink.next(JSONUtil.toJsonStr(toolExecutedMessage));
                    })
                    .onError((e) -> {
                        log.error("TokenStream error: {}", e.getMessage());
                        sink.error(e);
                    })
                    .onCompleteResponse((c) -> {
                        sink.complete();
                    })
                    .start();
        });
    }

    /**
     * 给定一个 代码输出流，为其绑定 Side-effect operator，在流完成时，先将ai输出流parse为pojo，再将代码保存到文件中并打印文件路径
     * @param codeFlux 包含代码的字符流
     * @param codeGenTypeEnum 代码生成类型
     * @return 处理过后的流
     */
    private Flux<String> processFileSave(Flux<String> codeFlux, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        StringBuilder sb = new StringBuilder();
        return codeFlux.doOnNext(sb::append).doOnComplete(() -> {
            try {
                Object codeResult = CodeParserExecutor.parse(sb.toString(), codeGenTypeEnum);
                File file = CodeFileSaverExecutor.saveFile(codeResult, codeGenTypeEnum, appId);
                assert file != null;
                log.info("文件保存路径: {}",file.getAbsolutePath());
            } catch (Exception e) {
                log.error("文件保存失败: {}", e.getMessage());
            }
        });
    }
}
