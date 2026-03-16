package cn.edu.sxu.museai.graph.ai;

import cn.edu.sxu.museai.graph.model.ImageCollectionPlan;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface ImageCollectionPlanService {

    /**
     * 根据用户提示词分析需要收集的图片类型和参数
     */
    @SystemMessage(fromResource = "prompt/ImageCollectionPlanPrompt.md")
    ImageCollectionPlan planImageCollection(@UserMessage String userPrompt);
}
