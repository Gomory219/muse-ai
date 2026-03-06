package cn.edu.sxu.museai.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * 更新应用名称请求（普通用户）
 */
@Data
public class AppNameUpdateRequest implements Serializable {

    /**
     * 应用id
     */
    private Long id;

    /**
     * 应用名称
     */
    private String appName;

    private static final long serialVersionUID = 1L;
}
