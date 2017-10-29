package com.tttx.demo.gtob;

public class CallOperation extends BaseActivity {

    private final String operation;

    public CallOperation(String obj, String operation) {
        this.setId(obj + "-" + operation);
        this.operation = operation;
    }

    public String getOperation() {
        return operation;
    }
}
