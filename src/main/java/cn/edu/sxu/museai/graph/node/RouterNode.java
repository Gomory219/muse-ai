package cn.edu.sxu.museai.graph.node;

import cn.edu.sxu.museai.ai.SmartRouteService;
import cn.edu.sxu.museai.graph.state.WorkflowContext;
import cn.edu.sxu.museai.model.enums.CodeGenTypeEnum;
import cn.edu.sxu.museai.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
public class RouterNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: 智能路由");

            CodeGenTypeEnum generationType;
            try {
                // 获取AI路由服务
                SmartRouteService routingService = SpringContextUtil.getBean(SmartRouteService.class);
                // 根据原始提示词进行智能路由
                generationType = routingService.decide(context.getOriginalPrompt());
                log.info("AI智能路由完成，选择类型: {} ({})", generationType.getValue(), generationType.getText());
            } catch (Exception e) {
                log.error("AI智能路由失败，使用默认HTML类型: {}", e.getMessage());
                generationType = CodeGenTypeEnum.HTML;
            }

            // 更新状态
            context.setCurrentStep("智能路由");
            context.setGenerationType(generationType);
            return WorkflowContext.saveContext(context);
        });
    }
}
