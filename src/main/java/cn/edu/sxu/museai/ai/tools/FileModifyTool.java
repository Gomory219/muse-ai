package cn.edu.sxu.museai.ai.tools;

import cn.edu.sxu.museai.exception.ThrowUtils;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import dev.langchain4j.service.MemoryId;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class FileModifyTool extends BaseTool {


    @Tool("修改文件内容，目前仅支持精确替换")
    public String modifyFile(@P("希望修改文件的相对路径") String filePath,
                             @P("希望替换的原始内容") String srcContent,
                             @P("希望替换后的内容") String destContent,
                             @ToolMemoryId Long appId) {
        String projectRootPath = projectRootPath(appId);
        String absoluteFilePath = projectRootPath + "/" + filePath;

        File file = new File(absoluteFilePath);

        ThrowUtils.throwIf(!file.exists(), new RuntimeException("文件不存在"));
        ThrowUtils.throwIf(file.isDirectory(), new RuntimeException("不允许修改目录"));

        try {
            String fileContent = FileUtil.readUtf8String(file);
            ThrowUtils.throwIf(fileContent.contains(srcContent), new RuntimeException("文件内容中不包含要替换的内容"));
            fileContent = fileContent.replace(srcContent, destContent);
            FileUtil.writeUtf8String(fileContent, file);
        } catch (IORuntimeException e) {
            throw new RuntimeException("文件修改失败: " + filePath);
        }
        return "文件修改成功" + filePath;
    }

    @Override
    public String getToolName() {
        return "modifyFile";
    }

    @Override
    public String getToolDescription() {
        return "修改文件内容";
    }

    @Override
    public String getResultDescription(JSONObject json) {
        String filePath = json.get("filePath", String.class);
        String srcContent = json.get("srcContent", String.class);
        String destContent = json.get("destContent", String.class);
        return "文件修改成功：" + filePath + "，原始内容：" + srcContent + "，替换后内容：" + destContent;
    }
}
