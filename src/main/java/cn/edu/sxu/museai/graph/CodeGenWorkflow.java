package cn.edu.sxu.museai.graph;

import cn.edu.sxu.museai.exception.BusinessException;
import cn.edu.sxu.museai.exception.ErrorCode;
import cn.edu.sxu.museai.graph.model.QualityResult;
import cn.edu.sxu.museai.graph.node.*;
import cn.edu.sxu.museai.graph.state.WorkflowContext;
import cn.edu.sxu.museai.model.enums.CodeGenTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.*;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.bsc.langgraph4j.prebuilt.MessagesStateGraph;

import java.util.Map;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;

@Slf4j
public class CodeGenWorkflow {

    /**
     * 创建完整的工作流
     */
    public CompiledGraph<MessagesState<String>> createWorkflow() {
        try {
            return new MessagesStateGraph<String>()
                    // 添加节点 - 使用完整实现的节点
                    .addNode("image_collector", ImageCollectorNode.create())
                    .addNode("prompt_enhancer", PromptEnhancerNode.create())
                    .addNode("router", RouterNode.create())
                    .addNode("code_generator", CodeGeneratorNode.create())
                    .addNode("project_builder", ProjectBuilderNode.create())
                    .addNode("code_quality_check", CodeQualityCheckNode.create())

                    // 添加边
                    .addEdge(START, "image_collector")
                    .addEdge("image_collector", "prompt_enhancer")
                    .addEdge("prompt_enhancer", "router")
                    .addEdge("router", "code_generator")
                    .addEdge("code_generator", "code_quality_check")
                    .addConditionalEdges("code_quality_check",
                            edge_async(this::routeAfterQualityCheck),
                            Map.of("build", "project_builder", "skip", END, "fail", "code_quality_check"))
                    .addEdge("project_builder", END)
                    // 编译工作流
                    .compile();
        } catch (GraphStateException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, e.getMessage());
        }
    }

    private String routeAfterQualityCheck(MessagesState<String> state) {
        WorkflowContext context = WorkflowContext.getContext(state);
        CodeGenTypeEnum generationType = context.getGenerationType();
        QualityResult qualityResult = context.getQualityResult();
        if (!qualityResult.getIsValid()) {
            return "fail";
        }
        return generationType == CodeGenTypeEnum.VUE ? "build" : "skip";
    }

    /**
     * 执行工作流
     */
    public WorkflowContext executeWorkflow(String originalPrompt) {
        CompiledGraph<MessagesState<String>> workflow = createWorkflow();

        // 初始化 WorkflowContext
        WorkflowContext initialContext = WorkflowContext.builder()
                .originalPrompt(originalPrompt)
                .currentStep("初始化")
                .build();

        GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
        log.info("工作流图:\n{}", graph.content());
        log.info("开始执行代码生成工作流");

        WorkflowContext finalContext = null;
        int stepCounter = 1;
        for (NodeOutput<MessagesState<String>> step : workflow.stream(
                Map.of(WorkflowContext.WORKFLOW_CONTEXT_KEY, initialContext))) {
            log.info("--- 第 {} 步完成 ---", stepCounter);
            // 显示当前状态
            WorkflowContext currentContext = WorkflowContext.getContext(step.state());
            if (currentContext != null) {
                finalContext = currentContext;
                log.info("当前步骤上下文: {}", currentContext);
            }
            stepCounter++;
        }
        log.info("代码生成工作流执行完成！");
        return finalContext;
    }
}
