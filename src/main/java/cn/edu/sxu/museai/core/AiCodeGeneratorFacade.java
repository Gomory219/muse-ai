package cn.edu.sxu.museai.core;

import cn.edu.sxu.museai.ai.AiService;
import cn.edu.sxu.museai.ai.model.HtmlCodeResult;
import cn.edu.sxu.museai.ai.model.MultiFileResult;
import cn.edu.sxu.museai.ai.parser.AiCodeResponseParser;
import cn.edu.sxu.museai.exception.ErrorCode;
import cn.edu.sxu.museai.exception.ThrowUtils;
import cn.edu.sxu.museai.model.enums.CodeGenTypeEnum;
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

    public File generateCodeAndSave(String userMessage, CodeGenTypeEnum codeGenTypeEnum) {
        ThrowUtils.throwIf(codeGenTypeEnum == null, ErrorCode.PARAMS_ERROR, "请选择代码生成模式");
        return switch (codeGenTypeEnum) {
            case HTML -> generateAndSaveHtmlCode(userMessage);
            case MULTI_FILE -> generateAndSaveMultiFile(userMessage);
        };
    }

    public Flux<String> generateCodeAndSaveStreaming(String userMessage, CodeGenTypeEnum codeGenTypeEnum) {
        ThrowUtils.throwIf(codeGenTypeEnum == null, ErrorCode.PARAMS_ERROR, "请选择代码生成模式");
        return switch (codeGenTypeEnum) {
            case HTML -> generateAndSaveHtmlStreaming(userMessage);
            case MULTI_FILE -> generateAndMultiFileStreaming(userMessage);
        };
    }

    private Flux<String> generateAndSaveHtmlStreaming(String userMessage) {
        Flux<String> stringFlux = aiService.generateSingleFileStreaming(userMessage);
        StringBuilder sb = new StringBuilder();
        return stringFlux.doOnNext(sb::append).doOnComplete(() -> {
            try {
                HtmlCodeResult htmlCodeResult = AiCodeResponseParser.parseSingleFile(sb.toString());
                File file = CodeFileSaver.saveCode(htmlCodeResult);
                log.info("文件保存路径: {}",file.getAbsolutePath());
            } catch (Exception e) {
                log.error("文件保存失败: {}", e.getMessage());
            }
        });
    }

/**
 * 生成并处理多文件流式响应
 * @param userMessage 用户输入的消息
 * @return 返回一个字符串类型的Flux流，包含生成的代码内容
 */
    private Flux<String> generateAndMultiFileStreaming(String userMessage) {
    // 调用AI服务生成多文件流式响应
        Flux<String> stringFlux = aiService.generateMultiFileStreaming(userMessage);
    // 使用StringBuilder来累积流中的字符串内容
        StringBuilder sb = new StringBuilder();
    // 处理流中的每个元素，将其追加到StringBuilder中
        return stringFlux.doOnNext(sb::append).doOnComplete(() -> {
            try {
            // 当流完成时，解析累积的字符串为多文件结果对象
                MultiFileResult multiFileResult = AiCodeResponseParser.parseMultiFile(sb.toString());
            // 将解析后的代码保存到文件系统
                File file = CodeFileSaver.saveCode(multiFileResult);
            // 记录文件保存成功的日志
                log.info("文件保存路径: {}", file.getAbsolutePath());
            } catch (Exception e) {
            // 记录文件保存失败的错误日志
                log.error("文件保存失败: {}", e.getMessage());
            }
        });
    }

    private File generateAndSaveHtmlCode(String userMessage) {
        HtmlCodeResult htmlCodeResult = aiService.generateSingleFile(userMessage);
        return CodeFileSaver.saveCode(htmlCodeResult);
    }

    private File generateAndSaveMultiFile(String userMessage) {
        MultiFileResult multiFileResult = aiService.generateMultiFile(userMessage);
        return CodeFileSaver.saveCode(multiFileResult);
    }
}
