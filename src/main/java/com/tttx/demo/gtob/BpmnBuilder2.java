package com.tttx.demo.gtob;

import com.google.common.graph.MutableGraph;
import org.activiti.bpmn.model.*;
import org.activiti.bpmn.model.Process;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BpmnBuilder2 {
    private final MutableGraph<BaseActivity> graph;
    private Process process;
    private Map<String, FlowElement> flowElementMap;

    public BpmnBuilder2(MutableGraph<BaseActivity> graph) {
        this.graph = graph;
    }

    public BpmnModel build(String workflowId) {
        this.flowElementMap = new HashMap<>();
        this.process = new Process();
        this.process.setId(workflowId);

        this.graph.nodes().forEach(node -> {
            ServiceTask serviceTask = this.getOrCreateServiceTask(node);
            this.graph.successors(node).forEach(successor -> {
                ServiceTask successorServiceTask = this.getOrCreateServiceTask(successor);
                this.addSequenceFlow(serviceTask, successorServiceTask);
            });
        });
        this.analysisParallelGateway();
        this.analysisEndpoint();

        BpmnModel bpmnModel = new BpmnModel();
        bpmnModel.addProcess(this.process);
        return bpmnModel;
    }

    private void analysisEndpoint() {
        List<FlowNode> startNodes = this.flowElementMap.values().stream()
                .filter(it -> it instanceof FlowNode && ((FlowNode) it).getIncomingFlows().isEmpty())
                .map(it -> (FlowNode) it).collect(Collectors.toList());
        List<FlowNode> endNodes = this.flowElementMap.values().stream()
                .filter(it -> it instanceof FlowNode && ((FlowNode) it).getOutgoingFlows().isEmpty())
                .map(it -> (FlowNode) it).collect(Collectors.toList());

        if (startNodes.isEmpty() || endNodes.isEmpty()) {
            throw new RuntimeException("invalid workflow with cycles");
        }

        this.getOrCreateFlowElement("start", StartEvent.class, startEvent -> {
            if (startNodes.size() == 1) {
                this.addSequenceFlow(startEvent, startNodes.get(0));
            } else {
                this.getOrCreateFlowElement("fork", ParallelGateway.class, fork -> {
                    this.addSequenceFlow(startEvent, fork);
                    startNodes.forEach(flowNode -> this.addSequenceFlow(fork, flowNode));
                    return fork;
                });
            }
            return startEvent;
        });

        this.getOrCreateFlowElement("end", EndEvent.class, endEvent -> {
            if (endNodes.size() == 1) {
                this.addSequenceFlow(endNodes.get(0), endEvent);
            } else {
                this.getOrCreateFlowElement("join", ParallelGateway.class, join -> {
                    this.addSequenceFlow(join, endEvent);
                    endNodes.forEach(flowNode -> this.addSequenceFlow(flowNode, join));
                    return join;
                });
            }
            return endEvent;
        });
    }

    private void analysisParallelGateway() {
        List<Activity> activities = this.flowElementMap.values().stream()
                .filter(it -> it instanceof Activity)
                .map(it -> (Activity) it)
                .collect(Collectors.toList());
        activities.forEach(it -> {
            if (it.getIncomingFlows().size() > 1) {
                // need a join gateway
                this.getOrCreateFlowElement("join-of-" + it.getId(), ParallelGateway.class, join -> {
                    it.getIncomingFlows().forEach(incoming -> this.setTarget(join, incoming));
                    it.getIncomingFlows().clear();
                    this.addSequenceFlow(join, it);
                    return join;
                });
            }
            if (it.getOutgoingFlows().size() > 1) {
                // need a fork gateway
                this.getOrCreateFlowElement("fork-of-" + it.getId(), ParallelGateway.class, fork -> {
                    it.getOutgoingFlows().forEach(outgoing -> this.setSource(fork, outgoing));
                    it.getOutgoingFlows().clear();
                    this.addSequenceFlow(it, fork);
                    return fork;
                });
            }
        });
    }

    private void setSource(FlowNode source, SequenceFlow sequenceFlow) {
        sequenceFlow.setSourceRef(source.getId());
        sequenceFlow.setSourceFlowElement(source);
        source.getOutgoingFlows().add(sequenceFlow);
    }

    private void setTarget(FlowNode target, SequenceFlow sequenceFlow) {
        sequenceFlow.setTargetRef(target.getId());
        sequenceFlow.setTargetFlowElement(target);
        target.getIncomingFlows().add(sequenceFlow);
    }

    private void addSequenceFlow(FlowNode source, FlowNode target) {
        String flowId = source.getId() + "--" + target.getId();
        this.getOrCreateFlowElement(flowId, SequenceFlow.class, flow -> {
            this.setSource(source, flow);
            this.setTarget(target, flow);
            return flow;
        });
    }

    private ServiceTask getOrCreateServiceTask(BaseActivity baseActivity) {
        return this.getOrCreateFlowElement(baseActivity.getId(), ServiceTask.class, task -> {
            task.setName(baseActivity.getId());
            return task;
        });
    }

    @SuppressWarnings("unchecked")
    private <T extends FlowElement> T getOrCreateFlowElement(String id, Class<T> elementClass, Function<T, T> callWhenCreate) {
        return (T) this.flowElementMap.computeIfAbsent(id, s -> {
            try {
                T ele = elementClass.newInstance();
                ele.setId(id);
                this.process.addFlowElement(ele);
                return callWhenCreate.apply(ele);
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException("failed to create " + elementClass + " instance with id " + id + ", error: " + e.getMessage(), e);
            }
        });
    }
}
