package cn.edu.sxu.museai.service;

import cn.edu.sxu.museai.common.PageResult;
import cn.edu.sxu.museai.model.dto.history.HistoryQueryRequest;
import cn.edu.sxu.museai.model.enums.MessageTypeEnum;
import com.mybatisflex.core.service.IService;
import cn.edu.sxu.museai.model.entity.History;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

/**
 * 对话历史 服务层。
 *
 * @author OneFish
 * @since 2026-03-07
 */
public interface HistoryService extends IService<History> {
    boolean addChatHistory(String message, MessageTypeEnum messageTypeEnum, Long appId, Long userId);

    PageResult<History> getHistory(HistoryQueryRequest historyQueryRequest);

    int loadMessageToMemory(ChatMemoryStore chatMemoryStore, Long appId);
}
