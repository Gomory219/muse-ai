package cn.edu.sxu.museai.common;

import com.mybatisflex.core.paginate.Page;
import lombok.Data;

@Data
public class PageRequest {

    /**
     * 当前页号
     */
    private int pageNum = 1;

    /**
     * 页面大小
     */
    private int pageSize = 10;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序顺序（默认降序）
     */
    private String sortOrder = "descend";

    public Page toPage() {
        return new Page<>(pageNum, pageSize);
    }
}
