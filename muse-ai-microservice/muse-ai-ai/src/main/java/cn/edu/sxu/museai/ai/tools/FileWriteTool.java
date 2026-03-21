package cn.edu.sxu.museai.ai.tools;

import cn.edu.sxu.museai.constant.AppConstant;
import cn.edu.sxu.museai.model.enums.CodeGenTypeEnum;
import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONObject;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;



@Slf4j
@Component
public class FileWriteTool extends BaseTool {

    @Tool("将内容写入文件。调用后若文件不存在，则直接创建文件+写入文件的内容。如果文件存在，则覆盖文件中的内容")
    public String writeFile(@P("写入文件的相对路径") String relativePath,
                            @P("写入文件的内容，内容不能为空") String content,
                            @ToolMemoryId Long memoryId) {
        try {
            String absPath = AppConstant.CODE_BATH_PATH + "/" + CodeGenTypeEnum.VUE.getValue() + "/" + memoryId + "/" + relativePath;
            File file = new File(absPath);
            File parentDir = file.getParentFile();
            if(!parentDir.exists()) {
                FileUtil.mkdir(parentDir);
            }
            FileUtil.writeUtf8String(content, file);
            return "成功写入文件：" + relativePath;
        } catch (Exception e) {
            throw new RuntimeException("写入文件失败：" + e.getMessage());
        }
    }

    @Override
    public String getToolName() {
        return "writeFile";
    }

    @Override
    public String getToolDescription() {
        return "写入文件";
    }

    @Override
    public String getResultDescription(JSONObject json) {
        return "写入文件成功：" + json.get("relativePath");
    }
}
