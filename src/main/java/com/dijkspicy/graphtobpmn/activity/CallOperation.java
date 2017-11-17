package com.dijkspicy.graphtobpmn.activity;

public class CallOperation<T> extends ExecutableActivity<T> {
    private final String operation;

    public CallOperation(String id, T target, String operation) {
        super(id, target);
        this.operation = operation;
    }

    public String getOperation() {
        return operation;
    }
}
