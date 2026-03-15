package cn.edu.sxu.museai.ai.tools;

import cn.edu.sxu.museai.constant.AppConstant;
import cn.edu.sxu.museai.exception.ThrowUtils;
import cn.edu.sxu.museai.model.enums.CodeGenTypeEnum;
import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONObject;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import dev.langchain4j.service.MemoryId;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Set;

@Component
public class FileDeleteTool extends BaseTool {

    private final Set<String> importantFiles = Set.of("package.json", "package-lock.json", "yarn.lock", "pnpm-lock.yaml",
            "vite.config.js", "vite.config.ts", "vue.config.js",
            "tsconfig.json", "tsconfig.app.json", "tsconfig.node.json",
            "index.html", "main.js", "main.ts", "App.vue", ".gitignore", "README.md");

    /**
     * 删除文件，不允许删除重要文件
     * @param relativeFilePath 相对路径
     * @param appId 应用ID
     * @return 删除结果
     */
    @Tool
    public String deleteFile(@P("要删除的文件相对路径") String relativeFilePath, @ToolMemoryId Long appId) {
        ThrowUtils.throwIf(isImportantFile(relativeFilePath), new RuntimeException("不允许删除重要文件"));
        String absoluteFilePath = AppConstant.CODE_BATH_PATH + "/"
                            + CodeGenTypeEnum.VUE.getValue() + "/"
                            + appId + "/"
                            + relativeFilePath;
        File file = new File(absoluteFilePath);
        ThrowUtils.throwIf(!file.exists(), new RuntimeException("文件不存在"));
        ThrowUtils.throwIf(file.isDirectory(), new RuntimeException("不允许删除目录"));

        boolean del = FileUtil.del(file);
        return del ? "成功删除文件：" + relativeFilePath : "删除文件失败：" + relativeFilePath;
    }

    /**
     * 判断是否是重要文件，不允许删除
     */
    private boolean isImportantFile(String fileName) {
        return importantFiles.contains(fileName);
    }

    @Override
    public String getToolName() {
        return "deleteFile";
    }

    @Override
    public String getToolDescription() {
        return "删除文件";
    }

    @Override
    public String getResultDescription(JSONObject json) {
        Object path = json.get("relativeFilePath");
        return "文件删除成功：" + path;
    }
}
