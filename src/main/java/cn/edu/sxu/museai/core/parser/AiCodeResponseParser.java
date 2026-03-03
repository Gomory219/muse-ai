package cn.edu.sxu.museai.core.parser;

import cn.edu.sxu.museai.ai.model.HtmlCodeResult;
import cn.edu.sxu.museai.ai.model.MultiFileResult;
import lombok.Data;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI代码响应解析器
 * 用于解析AI输出的Markdown代码块，提取HTML、CSS、JavaScript代码
 *
 * 支持的格式：
 * 1. 单文件格式：```html ... ```
 * 2. 多文件格式：```html ... ```, ```css ... ```, ```javascript ... ```
 */
public class AiCodeResponseParser {

    /**
     * 代码块正则模式，匹配 ```lang content ```
     */
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```(\\w+)?\\s*\\n([\\s\\S]*?)```");

    /**
     * 解析结果
     */
    @Data
    public static class ParseResult {
        private HtmlCodeResult singleFileResult;
        private MultiFileResult multiFileResult;
        private String description;

        public boolean isSingleFile() {
            return singleFileResult != null;
        }

        public boolean isMultiFile() {
            return multiFileResult != null;
        }
    }

    /**
     * 解析AI响应
     *
     * @param aiResponse AI输出的完整文本
     * @return 解析结果
     */
    public static ParseResult parse(String aiResponse) {
        if (aiResponse == null || aiResponse.isBlank()) {
            return createEmptyResult();
        }

        // 提取所有代码块
        Matcher matcher = CODE_BLOCK_PATTERN.matcher(aiResponse);

        String htmlCode = null;
        String cssCode = null;
        String jsCode = null;
        StringBuilder descriptionBuilder = new StringBuilder();

        int lastEnd = 0;

        while (matcher.find()) {
            // 提取代码块前的文本作为描述的一部分
            if (matcher.start() > lastEnd) {
                String textBefore = aiResponse.substring(lastEnd, matcher.start()).trim();
                if (!textBefore.isEmpty()) {
                    descriptionBuilder.append(textBefore).append("\n");
                }
            }

            String lang = matcher.group(1);
            String code = matcher.group(2).trim();

            if (lang == null) {
                // 无语言标记，默认当作HTML
                htmlCode = code;
            } else {
                switch (lang.toLowerCase()) {
                    case "html":
                        htmlCode = code;
                        break;
                    case "css":
                        cssCode = code;
                        break;
                    case "javascript":
                    case "js":
                        jsCode = code;
                        break;
                    default:
                        // 其他语言暂不处理
                        break;
                }
            }

            lastEnd = matcher.end();
        }

        // 提取最后一个代码块后的文本
        if (lastEnd < aiResponse.length()) {
            String textAfter = aiResponse.substring(lastEnd).trim();
            if (!textAfter.isEmpty()) {
                descriptionBuilder.append(textAfter).append("\n");
            }
        }

        String description = descriptionBuilder.toString().trim();

        // 判断是单文件还是多文件模式
        ParseResult result = new ParseResult();
        result.setDescription(description);

        if (cssCode != null || jsCode != null) {
            // 多文件模式
            MultiFileResult multiFileResult = new MultiFileResult();
            multiFileResult.setHtmlCode(htmlCode);
            multiFileResult.setCssCode(cssCode);
            multiFileResult.setJavaScriptCode(jsCode);
            multiFileResult.setDescription(description);
            result.setMultiFileResult(multiFileResult);
        } else {
            // 单文件模式
            HtmlCodeResult singleFileResult = new HtmlCodeResult();
            singleFileResult.setHtmlCode(htmlCode);
            singleFileResult.setDescription(description);
            result.setSingleFileResult(singleFileResult);
        }

        return result;
    }

    /**
     * 仅解析为单文件结果
     *
     * @param aiResponse AI输出的完整文本
     * @return 单文件结果
     */
    public static HtmlCodeResult parseSingleFile(String aiResponse) {
        ParseResult result = parse(aiResponse);
        return result.getSingleFileResult() != null ? result.getSingleFileResult() : new HtmlCodeResult();
    }

    /**
     * 仅解析为多文件结果
     *
     * @param aiResponse AI输出的完整文本
     * @return 多文件结果
     */
    public static MultiFileResult parseMultiFile(String aiResponse) {
        ParseResult result = parse(aiResponse);
        return result.getMultiFileResult() != null ? result.getMultiFileResult() : new MultiFileResult();
    }

    /**
     * 创建空结果
     */
    private static ParseResult createEmptyResult() {
        ParseResult result = new ParseResult();
        HtmlCodeResult emptySingle = new HtmlCodeResult();
        emptySingle.setHtmlCode("");
        emptySingle.setDescription("");
        result.setSingleFileResult(emptySingle);
        return result;
    }

    /**
     * 从文本中提取指定语言的代码块
     *
     * @param text      输入文本
     * @param language 代码语言 (html, css, javascript)
     * @return 代码内容，如果未找到返回null
     */
    public static String extractCodeBlock(String text, String language) {
        if (text == null || language == null) {
            return null;
        }

        Pattern pattern = Pattern.compile("```" + language + "\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return null;
    }

    /**
     * 移除所有代码块，返回纯文本描述
     *
     * @param text 输入文本
     * @return 移除代码块后的文本
     */
    public static String extractDescription(String text) {
        if (text == null) {
            return "";
        }

        return CODE_BLOCK_PATTERN.matcher(text).replaceAll("").trim();
    }
}
