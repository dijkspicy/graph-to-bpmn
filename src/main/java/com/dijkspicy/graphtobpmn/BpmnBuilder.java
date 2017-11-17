package com.dijkspicy.graphtobpmn;

import com.dijkspicy.graphtobpmn.activity.*;
import com.google.common.graph.MutableGraph;
import org.activiti.bpmn.BpmnAutoLayout;
import org.activiti.bpmn.model.*;
import org.activiti.bpmn.model.Process;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class BpmnBuilder<T> {
    private final MutableGraph<BaseActivity> graph;
    private final Collection<BaseActivity> nodes;
    private Process process;

    public BpmnBuilder(MutableGraph<BaseActivity> graph) {
        this.graph = graph;
        this.nodes = new ArrayList<>(this.graph.nodes());
    }

    public BpmnModel buildWithDI(String workflowId) {
        BpmnModel bpmnModel = this.build(workflowId);
        new BpmnAutoLayout(bpmnModel).execute();
        return bpmnModel;
    }

    public BpmnModel build(String workflowId) {
        this.process = new Process();
        BpmnModel bpmnModel = new BpmnModel();
        bpmnModel.addProcess(this.process);
        this.process.setId(workflowId);
        if (this.nodes.isEmpty()) {
            return bpmnModel;
        }

        this.analysisBranches();
        this.analysisEndpoint();

        this.graph.nodes().forEach(node -> {
            FlowNode serviceTask = this.getOrCreateNode(node);
            this.graph.successors(node).forEach(successor -> {
                FlowNode successorServiceTask = this.getOrCreateNode(successor);
                this.addSequenceFlow(serviceTask, successorServiceTask);
            });
        });

        return bpmnModel;
    }

    protected abstract void complete(ServiceTask serviceTask, SetState<T> baseActivity);

    protected abstract void complete(ServiceTask serviceTask, CallOperation<T> baseActivity);

    private void analysisBranches() {
        for (BaseActivity node : this.nodes) {
            List<BaseActivity> predecessors = new ArrayList<>(this.graph.predecessors(node));
            if (predecessors.size() > 1) {
                BaseActivity join = new Join("join-of-" + node.getId());
                for (BaseActivity predecessor : predecessors) {
                    this.graph.removeEdge(predecessor, node);
                    this.graph.putEdge(predecessor, join);
                }
                this.graph.putEdge(join, node);
            }

            List<BaseActivity> successors = new ArrayList<>(this.graph.successors(node));
            if (successors.size() > 1) {
                BaseActivity fork = new Fork("fork-of-" + node.getId());
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
            throw new BpmnBuildException("invalid workflow graph with cycles");
        }

        BaseActivity start = new Start("start");
        if (startNodes.size() == 1) {
            this.graph.putEdge(start, startNodes.get(0));
        } else {
            BaseActivity fork = new Fork("fork");
            this.graph.putEdge(start, fork);
            startNodes.forEach(it -> this.graph.putEdge(fork, it));
        }

        BaseActivity end = new End("end");
        if (endNodes.size() == 1) {
            this.graph.putEdge(endNodes.get(0), end);
        } else {
            BaseActivity join = new Join("join");
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
        });
    }

    @SuppressWarnings("unchecked")
    private FlowNode getOrCreateNode(BaseActivity baseActivity) {
        if (baseActivity instanceof Start) {
            return this.getOrCreateFlowElement(baseActivity.getId(), StartEvent.class, t -> {
            });
        }
        if (baseActivity instanceof End) {
            return this.getOrCreateFlowElement(baseActivity.getId(), EndEvent.class, t -> {
            });
        }
        if (baseActivity instanceof Fork || baseActivity instanceof Join) {
            return this.getOrCreateFlowElement(baseActivity.getId(), ParallelGateway.class, t -> {
            });
        }
        return this.getOrCreateFlowElement(baseActivity.getId(), ServiceTask.class, t -> {
            t.setName(baseActivity.getName());
            if (baseActivity instanceof SetState) {
                this.complete(t, (SetState<T>) baseActivity);
            } else if (baseActivity instanceof CallOperation) {
                this.complete(t, (CallOperation<T>) baseActivity);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private <F extends FlowElement> F getOrCreateFlowElement(String id, Class<F> elementClass, Consumer<F> callWhenCreate) {
        return (F) Optional.ofNullable(this.process.getFlowElement(id))
                .orElseGet(() -> {
                    try {
                        F ele = elementClass.newInstance();
                        ele.setId(id);
                        this.process.addFlowElement(ele);
                        callWhenCreate.accept(ele);
                        return ele;
                    } catch (InstantiationException | IllegalAccessException e) {
                        throw new BpmnBuildException("failed to create " + elementClass + " instance with id " + id + ", error: " + e.getMessage(), e);
                    }
                });
    }
}
