package cn.edu.sxu.museai.controller;

import jakarta.annotation.Resource;
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


}
