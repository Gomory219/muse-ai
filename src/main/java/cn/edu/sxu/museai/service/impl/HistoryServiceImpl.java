package cn.edu.sxu.museai.service.impl;

import cn.edu.sxu.museai.common.PageResult;
import cn.edu.sxu.museai.exception.ErrorCode;
import cn.edu.sxu.museai.exception.ThrowUtils;
import cn.edu.sxu.museai.model.dto.history.HistoryQueryRequest;
import cn.edu.sxu.museai.model.enums.MessageTypeEnum;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import cn.edu.sxu.museai.model.entity.History;
import cn.edu.sxu.museai.mapper.HistoryMapper;
import cn.edu.sxu.museai.service.HistoryService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话历史 服务层实现。
 *
 * @author OneFish
 * @since 2026-03-07
 */
@Service
@Slf4j
public class HistoryServiceImpl extends ServiceImpl<HistoryMapper, History>  implements HistoryService{

    @Override
    public boolean addChatHistory(String message, MessageTypeEnum messageTypeEnum, Long appId, Long userId) {
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "message不能为空");
        ThrowUtils.throwIf(appId == null, ErrorCode.PARAMS_ERROR, "appId不能为空");
        ThrowUtils.throwIf(userId == null, ErrorCode.PARAMS_ERROR, "userId不能为空");
        ThrowUtils.throwIf(messageTypeEnum == null, ErrorCode.PARAMS_ERROR, "messageTypeEnum不能为空");
        History history = History.builder()
                .message(message)
                .appId(appId)
                .userId(userId)
                .messageType(messageTypeEnum)
                .build();
        return save(history);
    }

    @Override
    public PageResult<History> getHistory(HistoryQueryRequest historyQueryRequest) {
        Long appId = historyQueryRequest.getAppId();
        LocalDateTime lastCreateTime = historyQueryRequest.getLastCreateTime();
        ThrowUtils.throwIf(appId == null, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        QueryWrapper queryWrapper = QueryWrapper.create();
        queryWrapper.eq(History::getAppId, appId)
                .lt(History::getCreateTime, lastCreateTime, lastCreateTime != null)
                .orderBy(History::getCreateTime, false);
        Page<History> page = page(Page.of(1,historyQueryRequest.getPageSize()), queryWrapper);
        return PageResult.page(page);
    }

    @Override
    public int loadMessageToMemory(ChatMemoryStore chatMemoryStore, Long appId) {
        try {
            QueryWrapper queryWrapper = QueryWrapper.create();
            queryWrapper.eq(History::getAppId, appId);
            queryWrapper.orderBy(History::getCreateTime, false);
            queryWrapper.limit(1, 8);
            List<History> list = list(queryWrapper);
            if (list.isEmpty()) {
                return 0;
            }
            list = list.reversed();
            List<ChatMessage> messages = list.stream().map(history ->
                    history.getMessageType() == MessageTypeEnum.AI ?
                            AiMessage.from(history.getMessage()) :
                            UserMessage.from(history.getMessage()))
                    .toList();
            chatMemoryStore.updateMessages(appId, messages);
            return list.size();
        } catch (Exception e) {
            log.error("加载对话记忆失败", e);
            return 0;
        }
    }
}














