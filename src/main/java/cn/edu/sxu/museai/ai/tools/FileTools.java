package cn.edu.sxu.museai.ai.tools;

import cn.edu.sxu.museai.constant.AppConstant;
import cn.edu.sxu.museai.model.enums.CodeGenTypeEnum;
import cn.hutool.core.io.FileUtil;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;

import java.io.File;



@Slf4j
public class FileTools {
    @Tool("将内容写入文件")
    public String writeFile(@P("写入文件的相对路径") String relativePath,
                            @P("写入文件的内容") String content,
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
            return "写入文件失败：" + e.getMessage();
        }
    }
}
