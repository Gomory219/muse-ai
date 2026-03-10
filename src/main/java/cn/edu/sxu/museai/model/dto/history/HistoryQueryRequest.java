package cn.edu.sxu.museai.model.dto.history;

import cn.edu.sxu.museai.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
public class HistoryQueryRequest extends PageRequest implements Serializable {

    /**
     * 应用id
     */
    private Long appId;

    /**
     * 游标查询 - 最后一条记录的id
     * 用于分页查询，获取小于该id的记录
     */
    private Long lastId;

    @Serial
    private static final long serialVersionUID = 1L;
}
