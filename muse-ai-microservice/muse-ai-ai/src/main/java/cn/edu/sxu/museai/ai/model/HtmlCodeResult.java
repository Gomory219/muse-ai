package cn.edu.sxu.museai.ai.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

@Data
@Description("生成 HTML 代码的结果")
public class HtmlCodeResult {
    @Description("HTML 代码")
    String HtmlCode;
    @Description("生成代码的描述")
    String description;
}
