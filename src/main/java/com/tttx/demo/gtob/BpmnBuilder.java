package com.tttx.demo.gtob;

import com.google.common.graph.MutableGraph;
import org.activiti.bpmn.model.*;
import org.activiti.bpmn.model.Process;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BpmnBuilder {
    private final MutableGraph<BaseActivity> graph;
    private final List<BaseActivity> nodes;
    private Process process;
    private Map<String, FlowElement> flowElementMap;

    public BpmnBuilder(MutableGraph<BaseActivity> graph) {
        this.graph = graph;
        this.nodes = new ArrayList<>(graph.nodes());
    }

    public BpmnModel build(String workflowId) {
        this.analysisBranches();
        this.analysisEndpoint();

        this.flowElementMap = new HashMap<>();
        this.process = new Process();
        this.process.setId(workflowId);
        this.graph.nodes().forEach(node -> {
            FlowNode serviceTask = this.getOrCreateNode(node);
            this.graph.successors(node).forEach(successor -> {
                FlowNode successorServiceTask = this.getOrCreateNode(successor);
                this.addSequenceFlow(serviceTask, successorServiceTask);
            });
        });

        BpmnModel bpmnModel = new BpmnModel();
        bpmnModel.addProcess(this.process);
        return bpmnModel;
    }

    private void analysisBranches() {
        for (BaseActivity node : this.nodes) {
            List<BaseActivity> predecessors = new ArrayList<>(this.graph.predecessors(node));
            if (predecessors.size() > 1) {
                BaseActivity join = new Join().setId("join-of-" + node.getId());
                for (BaseActivity predecessor : predecessors) {
                    this.graph.removeEdge(predecessor, node);
                    this.graph.putEdge(predecessor, join);
                }
                this.graph.putEdge(join, node);
            }

            List<BaseActivity> successors = new ArrayList<>(this.graph.successors(node));
            if (successors.size() > 1) {
                BaseActivity fork = new Fork().setId("fork-of-" + node.getId());
                for (BaseActivity successor : successors) {
                    this.graph.removeEdge(node, successor);
                    this.graph.putEdge(fork, successor);
                }
                this.graph.putEdge(node, fork);
            }
        }
    }

    private void analysisEndpoint() {
        List<BaseActivity> startNodes = this.nodes.stream().filter(it -> this.graph.predecessors(it).isEmpty()).collect(Collectors.toList());
        List<BaseActivity> endNodes = this.nodes.stream().filter(it -> this.graph.successors(it).isEmpty()).collect(Collectors.toList());
        if (startNodes.isEmpty() || endNodes.isEmpty()) {
            throw new RuntimeException("invalid workflow graph with cycles");
        }

        BaseActivity start = new Start().setId("start");
        if (startNodes.size() == 1) {
            this.graph.putEdge(start, startNodes.get(0));
        } else {
            BaseActivity fork = new Fork().setId("fork");
            this.graph.putEdge(start, fork);
            startNodes.forEach(it -> this.graph.putEdge(fork, it));
        }

        BaseActivity end = new End().setId("end");
        if (endNodes.size() == 1) {
            this.graph.putEdge(endNodes.get(0), end);
        } else {
            BaseActivity join = new Join().setId("join");
            this.graph.putEdge(join, end);
            endNodes.forEach(it -> this.graph.putEdge(it, join));
        }
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

    private FlowNode getOrCreateNode(BaseActivity baseActivity) {
        if (baseActivity instanceof Start) {
            return this.getOrCreateFlowElement(baseActivity.getId(), StartEvent.class, t -> t);
        }
        if (baseActivity instanceof End) {
            return this.getOrCreateFlowElement(baseActivity.getId(), EndEvent.class, t -> t);
        }
        if (baseActivity instanceof Fork || baseActivity instanceof Join) {
            return this.getOrCreateFlowElement(baseActivity.getId(), ParallelGateway.class, t -> t);
        }
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
