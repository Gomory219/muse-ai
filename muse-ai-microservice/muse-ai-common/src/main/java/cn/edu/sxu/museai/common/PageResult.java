package cn.edu.sxu.museai.common;

import com.mybatisflex.core.paginate.Page;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PageResult<T> {
    private Long pageNum;
    private Long pageSize;
    private Long total;
    private List<T> list;

    public static<T> PageResult<T> page(Page<T> page){
        return PageResult.<T>builder()
                .pageNum(page.getPageNumber())
                .pageSize(page.getPageSize())
                .total(page.getTotalRow())
                .list(page.getRecords())
                .build();
    }

    public static<T> PageResult<T> page(Page<?> page, List<T> list){
        return PageResult.<T>builder()
                .pageNum(page.getPageNumber())
                .pageSize(page.getPageSize())
                .total(page.getTotalRow())
                .list(list)
                .build();
    }

}
