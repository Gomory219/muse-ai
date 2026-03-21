package cn.edu.sxu.museai.ai.tools;

import cn.edu.sxu.museai.exception.ThrowUtils;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.json.JSONObject;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import dev.langchain4j.service.MemoryId;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class FileReadTool extends BaseTool {

    @Tool("读取文件")
    public String readFile(@P("希望读取文件的相对路径") String filePath, @ToolMemoryId Long appId) {
        try {
            String rootPath = this.projectRootPath(appId);
            String absoluteFilePath = rootPath + "/" + filePath;
            File file = new File(absoluteFilePath);
            ThrowUtils.throwIf(!file.exists(), new RuntimeException("文件不存在"));
            ThrowUtils.throwIf(file.isDirectory(), new RuntimeException("不能读取目录"));

            return FileUtil.readUtf8String(file);
        } catch (IORuntimeException e) {
            throw new RuntimeException("读取文件失败: " + filePath);
        }
    }

    @Override
    public String getToolName() {
        return "readFile";
    }

    @Override
    public String getToolDescription() {
        return "读取文件内容";
    }

    @Override
    public String getResultDescription(JSONObject json) {
        return "读取文件内容成功：" + json.get("filePath");
    }
}
