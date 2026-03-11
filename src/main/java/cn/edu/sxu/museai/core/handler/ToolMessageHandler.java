package cn.edu.sxu.museai.core.handler;

import cn.edu.sxu.museai.ai.model.message.*;
import cn.edu.sxu.museai.constant.AppConstant;
import cn.edu.sxu.museai.core.builder.VueProjectBuilder;
import cn.edu.sxu.museai.model.entity.History;
import cn.edu.sxu.museai.model.enums.CodeGenTypeEnum;
import cn.edu.sxu.museai.model.enums.MessageTypeEnum;
import cn.edu.sxu.museai.service.HistoryService;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.openai.internal.chat.Message;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.*;

/**
 * 带有工具调用消息的处理器
 * 带有工具调用的流，上游共会传下来三种类型的消息：AI_RESPONSE、TOOL_REQUEST、TOOL_EXECUTED
 * AI_RESPONSE：AI的响应消息                                      相应给前端 + 保存到数据库
 * TOOL_REQUEST：工具调用请求消息                                  相应给前端
 * TOOL_EXECUTED：工具调用执行结果消息 调用了哪些工具 + 结果           相应给前端 + 保存到数据库
 */
@Slf4j
@Component
public class ToolMessageHandler extends BaseMessageHandler {
    @Resource
    private HistoryService historyService;
    @Resource
    private VueProjectBuilder vueProjectBuilder;

    /**
     * AI 生成的内容如：
     * response
     * tool_request
     * 到此，工具开始执行
     * ---------------
     * 经过上游处理过后，现在的流变为：
     * response      下一轮的ai调用时，上一轮的所有工具调用已经结束，可以将所有工具调用结果保存到aiMessage中
     * tool_request
     * tool_executed 在此处，ai的流已经生成完毕，第一次调用工具时，可以将aiResponse保存为AiMessage，并且插入historyList，此外，每次调用工具后，将工具调用的结果也加入List
     *
     * @param codeStream      流式消息
     * @param appId           应用ID
     * @param codeGenTypeEnum 代码生成类型
     * @param userId          用户ID
     * @return 处理后的消息
     */
    @Override
    public Flux<String> handle(Flux<String> codeStream, Long appId, CodeGenTypeEnum codeGenTypeEnum, Long userId) {
        StringBuilder aiResponseStringBuilder = new StringBuilder();
        Set<String> seenToolIds = new HashSet<>();
        List<History> historyList = new ArrayList<>();
        List<ToolExecutedMessage> toolExecutedMessageList = new ArrayList<>();
        return codeStream.map((message) -> processStreamMessage(aiResponseStringBuilder, message, seenToolIds, appId, userId, historyList, toolExecutedMessageList))
                .filter(StrUtil::isNotBlank)
                .doOnComplete(() -> {
                    History history = History.builder()
                        .message(aiResponseStringBuilder.toString())
                        .messageType(MessageTypeEnum.AI)
                        .appId(appId)
                        .userId(userId)
                        .build();
                    historyList.add(history);
                    historyService.saveBatch(historyList);
                    String projectPath = AppConstant.CODE_BATH_PATH + "/" + codeGenTypeEnum.getValue() + "/" + appId;
                    vueProjectBuilder.buildProject(projectPath);
                });
    }

    private String processStreamMessage(StringBuilder sb, String message, Set<String> seenToolIds, Long appId, Long userId, List<History> historyList, List<ToolExecutedMessage> toolExecutedMessageList) {
        StreamMessage streamMessage = JSONUtil.toBean(message, StreamMessage.class);
        switch (streamMessage.getType()) {
            case AI_RESPONSE -> {
                if (sb.isEmpty() && !toolExecutedMessageList.isEmpty()) {
                    // 从工具执行转到AI响应的首次，需要将上一次的工具调用结果保存
                    // 结果有两个地方需要用到，一个是上一次的ai响应，一个是工具调用结果
                    History lastHistory = historyList.getLast();
                    List<History> toolsHistoryList = toolExecutedMessageList.stream().map(toolExecutedMessage -> {
                        ToolExecutedMessage toolMessage = ToolExecutedMessage.builder()  // 近乎保留了原来消息的所有信息，包括名称、输出、参数
                                .toolName(toolExecutedMessage.getToolName())
                                .toolOutput(toolExecutedMessage.getToolOutput())
                                .arguments(toolExecutedMessage.getArguments())
                                .success(toolExecutedMessage.getSuccess())
                                .build();
                        return History.builder()
                                .message(JSONUtil.toJsonStr(toolMessage))
                                .messageType(MessageTypeEnum.TOOL_EXECUTED)
                                .appId(appId)
                                .userId(userId)
                                .build();
                    }).toList();
                    historyList.addAll(toolsHistoryList);
                    List<ToolExecutedMessage> toolsSaveToAiHistory = toolExecutedMessageList.stream().map(toolExecutedMessage -> {
                        ToolExecutedMessage messageOnlyName = new ToolExecutedMessage();
                        messageOnlyName.setToolName(toolExecutedMessage.getToolName());
                        return messageOnlyName;
                    }).toList();
                    lastHistory.setToolExecutionRequests(JSONUtil.toJsonStr(toolsSaveToAiHistory));
                    toolExecutedMessageList.clear();
                }
                ResponseStreamMessage response = JSONUtil.toBean(message, ResponseStreamMessage.class);
                sb.append(response.getResponse());
                StandardMessage standardMessage = StandardMessage.builder()
                        .jsonViewType(JsonViewType.TEXT)
                        .v(response.getResponse())
                        .build();
                return JSONUtil.toJsonStr(standardMessage);
            }
            case TOOL_REQUEST -> {
                ToolRequestMessage toolRequestMessage = JSONUtil.toBean(message, ToolRequestMessage.class);
                if (seenToolIds.contains(toolRequestMessage.getToolId())) {
                    return "";
                }
                seenToolIds.add(toolRequestMessage.getToolId());
                StandardMessage standardMessage = StandardMessage.builder()
                        .jsonViewType(JsonViewType.TOOL_REQUEST)
                        .toolName(toolRequestMessage.getToolName())
                        .build();
                return JSONUtil.toJsonStr(standardMessage);
            }
            case TOOL_EXECUTED -> {
                if (!sb.isEmpty()) {
                    History history = History.builder()
                            .message(sb.toString())
                            .messageType(MessageTypeEnum.AI)
                            .appId(appId)
                            .userId(userId)
                            .build();
                    historyList.add(history);
                    sb.setLength(0);
                }
                ToolExecutedMessage toolExecutedMessage = JSONUtil.toBean(message, ToolExecutedMessage.class);
                toolExecutedMessageList.add(toolExecutedMessage);
                StandardMessage standardMessage = StandardMessage.builder()
                        .jsonViewType(JsonViewType.TOOL_EXECUTED)
                        .toolName(toolExecutedMessage.getToolName())
                        .toolResult(toolExecutedMessage.getToolOutput())
                        .success(toolExecutedMessage.getSuccess())
                        .v(toolExecutedMessage.getArguments())
                        .build();
                return JSONUtil.toJsonStr(standardMessage);
            }
            default -> {
                return "";
            }
        }
    }

}
