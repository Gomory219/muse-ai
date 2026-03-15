package cn.edu.sxu.museai.graph.ai;

import cn.edu.sxu.museai.graph.model.QualityResult;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface CodeQualityCheckService {

    /**
     * 检查代码质量
     * AI 会分析代码并返回质量检查结果
     */
    @SystemMessage(fromResource = "prompt/CodeQualityCheckPrompt.md")
    QualityResult checkCodeQuality(@UserMessage String codeContent);
}
