package cn.edu.sxu.museai.ai;

import cn.edu.sxu.museai.ai.model.HtmlCodeResult;
import cn.edu.sxu.museai.ai.model.MultiFileResult;
import dev.langchain4j.service.SystemMessage;
import reactor.core.publisher.Flux;

public interface AiService {

    @SystemMessage(fromResource = "prompt/SingleFileCodePrompt.md")
    HtmlCodeResult generateSingleFile(String userMessage);

    @SystemMessage(fromResource = "prompt/MultiFileCodePrompt.md")
    MultiFileResult generateMultiFile(String userMessage);

    @SystemMessage(fromResource = "prompt/SingleFileCodePrompt.md")
    Flux<String> generateSingleFileStreaming(String userMessage);

    @SystemMessage(fromResource = "prompt/MultiFileCodePrompt.md")
    Flux<String> generateMultiFileStreaming(String userMessage);
}
