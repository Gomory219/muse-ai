package cn.edu.sxu.museai.core.handler;


import cn.edu.sxu.museai.model.enums.CodeGenTypeEnum;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 *  将上游的流，处理为可以直接范围给前端的流
 *  需要做的事情有：
 *  1. 将响应流绑定事件：当生成结束时，将对话存入数据库
 *  2. 将原始的上游文本数据（如果是Vue则为Json）转化为可以直接返回给前端的标准数据
 */
@Component
public class MessageHandlerExecutor {
    @Resource
    private SimpleMessageHandler simpleMessageHandler;
    @Resource
    private ToolMessageHandler toolMessageHandler;

    public Flux<String> execute(Flux<String> flux, Long appId, CodeGenTypeEnum codeGenTypeEnum, Long userId) {
         Flux<String> codeStream = switch (codeGenTypeEnum) {
            case VUE -> toolMessageHandler.handle(flux, appId, codeGenTypeEnum, userId);
            case MULTI_FILE,HTML -> simpleMessageHandler.handle(flux, appId, codeGenTypeEnum, userId);
        };
         return codeStream.concatWithValues(
                 JSONUtil.toJsonStr(StandardMessage.builder().jsonViewType(JsonViewType.FINISH).build()));
    }
}
