package cn.edu.sxu.museai.model.enums;

import lombok.Getter;

@Getter
public enum CodeGenTypeEnum {
    HTML("原生HTML模式","html"),
    MULTI_FILE("原生多文件模式","multi-file");

    private final String text;
    private final String value;

    CodeGenTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }
}
