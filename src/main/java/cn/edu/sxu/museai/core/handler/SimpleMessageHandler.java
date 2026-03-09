package cn.edu.sxu.museai.core.handler;

import cn.edu.sxu.museai.core.parser.CodeParserExecutor;
import cn.edu.sxu.museai.core.saver.CodeFileSaverExecutor;
import cn.edu.sxu.museai.model.enums.CodeGenTypeEnum;
import cn.edu.sxu.museai.model.enums.MessageTypeEnum;
import cn.edu.sxu.museai.service.HistoryService;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class SimpleMessageHandler extends BaseMessageHandler {

    @Resource
    private HistoryService historyService;

    @Override
    public Flux<String> handle(Flux<String> codeStream, Long appId, CodeGenTypeEnum codeGenTypeEnum, Long userId) {
        StringBuilder sb = new StringBuilder();
        return codeStream.doOnNext(sb::append).doFinally(signalType ->  {
            // 当最后一条消息处理完毕后，将代码保存到文件
            String content = sb.toString();
            Object codeResult = CodeParserExecutor.parse(content, codeGenTypeEnum);
            CodeFileSaverExecutor.saveFile(codeResult, codeGenTypeEnum, appId);
            // 保存代码生成历史
            historyService.addChatHistory(content, MessageTypeEnum.AI , appId, userId);
        }).map(chunk -> {
            // 在上游保存过记录后，最后一步再将内容转化为 JSON
            StandardMessage message = StandardMessage.builder()
                    .jsonViewType(JsonViewType.TEXT)
                    .v(chunk)
                    .build();
            return JSONUtil.toJsonStr(message);
        });
    }
}
