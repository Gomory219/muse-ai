package cn.edu.sxu.museai.core.handler;

/**
 * 这个枚举定义了前端接收的消息类型
 * 本来想定义为 MessageTypeEnum 但是已经存在该类了
 */
public enum JsonViewType {
    TEXT,
    FINISH,
    TOOL_REQUEST,
    TOOL_EXECUTED
}
