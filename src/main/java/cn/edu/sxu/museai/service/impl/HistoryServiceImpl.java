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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 对话历史 服务层实现。
 *
 * @author OneFish
 * @since 2026-03-07
 */
@Service
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
                .gt(History::getCreateTime, lastCreateTime, lastCreateTime != null);
        Page<History> page = page(Page.of(historyQueryRequest.getPageNum(), 1), queryWrapper);
        return PageResult.page(page);
    }
}














