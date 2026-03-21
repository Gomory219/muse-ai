package cn.edu.sxu.museai.controller;

import cn.edu.sxu.museai.common.BaseResponse;
import cn.edu.sxu.museai.common.PageResult;
import cn.edu.sxu.museai.common.ResultUtils;
import cn.edu.sxu.museai.model.dto.history.HistoryQueryRequest;
import cn.edu.sxu.museai.model.entity.History;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import cn.edu.sxu.museai.service.HistoryService;
import org.springframework.web.bind.annotation.RestController;

/**
 * 对话历史 控制层。
 *
 * @author OneFish
 * @since 2026-03-07
 */
@RestController
@RequestMapping("/history")
public class HistoryController {

    @Resource
    private HistoryService historyService;

    @GetMapping
    public BaseResponse<PageResult<History>> getHistory(HistoryQueryRequest historyQueryRequest) {
        return ResultUtils.success(historyService.getHistory(historyQueryRequest));
    }

}
