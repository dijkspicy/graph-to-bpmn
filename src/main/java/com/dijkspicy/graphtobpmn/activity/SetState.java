package com.dijkspicy.graphtobpmn.activity;

public class SetState<T> extends ExecutableActivity<T> {
    private final String state;

    public SetState(String id, T target, String state) {
        super(id, target);
        this.state = state;
    }

    public String getState() {
        return state;
    }
}
