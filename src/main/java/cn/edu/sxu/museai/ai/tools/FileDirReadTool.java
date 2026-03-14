package cn.edu.sxu.museai.ai.tools;

import cn.edu.sxu.museai.exception.ThrowUtils;
import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONObject;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.service.MemoryId;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.Set;

@Component
public class FileDirReadTool extends BaseTool {

    /**
     * 需要忽略的文件和目录
     */
    private static final Set<String> IGNORED_NAMES = Set.of(
            "node_modules", ".git", "dist", "build", ".DS_Store",
            ".env", "target", ".mvn", ".idea", ".vscode", "coverage"
    );

    /**
     * 需要忽略的文件扩展名
     */
    private static final Set<String> IGNORED_EXTENSIONS = Set.of(
            ".log", ".tmp", ".cache", ".lock"
    );


    @Tool
    public String readDir(@P("文件夹的相对路径，空则代表根目录") String relativePath, @MemoryId Long appId) {
        relativePath = relativePath == null ? "" : relativePath;
        String absolutePath = projectRootPath(appId) + "/" + relativePath;
        File dir = new File(absolutePath);
        ThrowUtils.throwIf(!dir.exists(), new RuntimeException("目录不存在"));
        ThrowUtils.throwIf(!dir.isDirectory(), new RuntimeException("不是目录"));

        StringBuilder structure = new StringBuilder();
        structure.append("项目目录结构:\n");
        structure.append(dir.getName()).append("/\n");

        buildTreeStructure(dir, structure, "", true);
        return structure.toString();
    }

    private boolean shouldIgnore(String name) {
        return IGNORED_NAMES.contains(name) || IGNORED_EXTENSIONS.contains(FileUtil.extName(name));
    }

    private void buildTreeStructure(File dir, StringBuilder structure, String prefix, boolean isLast) {
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            return;
        }

        // 过滤并排序：目录在前，文件在后
        List<File> filteredFiles = java.util.Arrays.stream(files)
                .filter(f -> !shouldIgnore(f.getName()))
                .sorted((a, b) -> {
                    boolean aIsDir = a.isDirectory();
                    boolean bIsDir = b.isDirectory();
                    if (aIsDir && !bIsDir) return -1;
                    if (!aIsDir && bIsDir) return 1;
                    return a.getName().compareTo(b.getName());
                })
                .toList();

        for (int i = 0; i < filteredFiles.size(); i++) {
            File file = filteredFiles.get(i);
            boolean last = (i == filteredFiles.size() - 1);

            // 构建当前行的前缀
            String connector = last ? "└── " : "├── ";
            structure.append(prefix).append(connector).append(file.getName());

            if (file.isDirectory()) {
                structure.append("/\n");
                // 构建下一层的缩进前缀
                String nextPrefix = prefix + (last ? "    " : "│   ");
                buildTreeStructure(file, structure, nextPrefix, true);
            } else {
                structure.append("\n");
            }
        }
    }

    @Override
    public String getToolName() {
        return "readDir";
    }

    @Override
    public String getToolDescription() {
        return "读取项目目录结构";
    }

    @Override
    public String getResultDescription(JSONObject json) {
        Object relativePath = json.get("relativePath");
        return "成功读取目录结构：" + relativePath;
    }
}
