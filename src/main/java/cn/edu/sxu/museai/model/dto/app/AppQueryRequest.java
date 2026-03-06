package cn.edu.sxu.museai.model.dto.app;

import cn.edu.sxu.museai.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 应用查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AppQueryRequest extends PageRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 应用名称（模糊查询）
     */
    private String appName;

    /**
     * 创建用户id
     */
    private Long userId;

    /**
     * 代码生成类型
     */
    private String codeGenType;

    /**
     * 最小优先级
     */
    private Integer minPriority;

    /**
     * 最大优先级
     */
    private Integer maxPriority;

    private static final long serialVersionUID = 1L;
}
