package cn.edu.sxu.museai.controller;

import cn.edu.sxu.museai.common.BaseResponse;
import cn.edu.sxu.museai.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    @GetMapping("/health")
    public BaseResponse<String> health() {
        return ResultUtils.success("Healthy");
    }
}
