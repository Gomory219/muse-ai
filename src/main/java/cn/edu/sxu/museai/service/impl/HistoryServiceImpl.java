package cn.edu.sxu.museai.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import cn.edu.sxu.museai.entity.History;
import cn.edu.sxu.museai.mapper.HistoryMapper;
import cn.edu.sxu.museai.service.HistoryService;
import org.springframework.stereotype.Service;

/**
 * 对话历史 服务层实现。
 *
 * @author OneFish
 * @since 2026-03-07
 */
@Service
public class HistoryServiceImpl extends ServiceImpl<HistoryMapper, History>  implements HistoryService{

}
