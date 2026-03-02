package cn.edu.sxu.museai.service;

import cn.edu.sxu.museai.ai.AiService;
import cn.edu.sxu.museai.ai.model.HtmlCodeResult;
import cn.edu.sxu.museai.ai.model.MultiFileResult;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AIServiceTest {

    @Resource
    private AiService aiService;

    @Test
    void generateSingleFile() {
        HtmlCodeResult result = aiService.generateSingleFile("请帮我生成一个技术博客主页，代码不超过100行");
        assert result != null;
    }

    @Test
    void generateMultiFile() {
        MultiFileResult result = aiService.generateMultiFile("请帮我生成一个技术博客主页，代码不超过100行");
        assert result != null;
    }
}