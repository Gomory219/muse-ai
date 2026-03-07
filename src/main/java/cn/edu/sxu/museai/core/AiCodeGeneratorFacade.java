package cn.edu.sxu.museai.core;

import cn.edu.sxu.museai.ai.AiService;
import cn.edu.sxu.museai.ai.model.HtmlCodeResult;
import cn.edu.sxu.museai.ai.model.MultiFileResult;
import cn.edu.sxu.museai.core.parser.AiCodeResponseParser;
import cn.edu.sxu.museai.core.parser.CodeParserExecutor;
import cn.edu.sxu.museai.core.parser.CodeParserStrategy;
import cn.edu.sxu.museai.core.saver.CodeFileSaver;
import cn.edu.sxu.museai.core.saver.CodeFileSaverExecutor;
import cn.edu.sxu.museai.exception.ErrorCode;
import cn.edu.sxu.museai.exception.ThrowUtils;
import cn.edu.sxu.museai.model.enums.CodeGenTypeEnum;
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
        };
        return CodeFileSaverExecutor.saveFile(codeResult, codeGenTypeEnum, appId);
    }

    /**
     * 根据用户信息和生成类型生成文件并保存为文件（流式）
     * @param userMessage 用户输入的消息，用于生成代码
     * @param codeGenTypeEnum 代码生成类型枚举，决定生成单文件还是多文件
     * @return AI输出流
     */
    public Flux<String> generateCodeAndSaveStreaming(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        ThrowUtils.throwIf(codeGenTypeEnum == null, ErrorCode.PARAMS_ERROR, "请选择代码生成模式");
        Flux<String> codeFlux = switch (codeGenTypeEnum) {
            case HTML -> aiService.generateSingleFileStreaming(userMessage);
            case MULTI_FILE -> aiService.generateMultiFileStreaming(userMessage);
        };
        return processFileSave(codeFlux, codeGenTypeEnum, appId);
    }

    /**
     * 给定一个 代码输出流，为其绑定 Side-effect operator，在流完成时，将代码保存到文件中并打印文件路径
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
                log.info("文件保存路径: {}",file.getAbsolutePath());
            } catch (Exception e) {
                log.error("文件保存失败: {}", e.getMessage());
            }
        });
    }
}
