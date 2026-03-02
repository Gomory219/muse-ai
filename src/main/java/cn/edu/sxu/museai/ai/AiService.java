package cn.edu.sxu.museai.ai;

import cn.edu.sxu.museai.ai.model.HtmlCodeResult;
import cn.edu.sxu.museai.ai.model.MultiFileResult;
import dev.langchain4j.service.SystemMessage;

public interface AiService {

    @SystemMessage(fromResource = "prompt/SingleFileCodePrompt.md")
    HtmlCodeResult generateSingleFile(String userMessage);

    @SystemMessage(fromResource = "prompt/MultiFileCodePrompt.md")
    MultiFileResult generateMultiFile(String userMessage);
}
