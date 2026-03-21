package cn.edu.sxu.museai.core.handler;

import cn.edu.sxu.museai.model.enums.CodeGenTypeEnum;
import cn.hutool.json.JSONUtil;
import reactor.core.publisher.Flux;

public abstract class BaseMessageHandler {
    protected abstract Flux<String> handle(Flux<String> codeStream, Long appId, CodeGenTypeEnum codeGenTypeEnum, Long userId);

    /**
     * 处理代码生成请求，在 handle的逻辑上补全了结束消息
     * @param codeStream 代码流
     * @param appId 应用ID
     * @param codeGenTypeEnum 代码生成类型
     * @param userId 用户ID
     * @return 处理结果
     */
    public final Flux<String> process(Flux<String> codeStream, Long appId, CodeGenTypeEnum codeGenTypeEnum, Long userId) {
        return handle(codeStream, appId, codeGenTypeEnum, userId)
                .concatWithValues(JSONUtil.toJsonStr(StandardMessage.finishMessage()));
    }
}
