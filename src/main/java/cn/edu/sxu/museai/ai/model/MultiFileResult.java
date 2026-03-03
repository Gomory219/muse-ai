package cn.edu.sxu.museai.ai.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

@Data
@Description("生成多个代码文件的结果")
public class MultiFileResult {
    @Description("HTML代码")
    String HtmlCode;
    @Description("JavaScript代码")
    String JavaScriptCode;
    @Description("Css代码")
    String CssCode;
    @Description("生成代码的描述")
    String description;
}
