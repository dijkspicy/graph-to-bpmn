package com.dijkspicy.graphtobpmn;

/**
 * graph-to-bpmn
 *
 * @Author dijkspicy
 * @Date 2017/11/13
 */
public class BpmnBuildException extends RuntimeException {
    public BpmnBuildException(String msg, Exception e) {
        super(msg, e);
    }

    public BpmnBuildException(String msg) {
        super(msg);
    }
}
