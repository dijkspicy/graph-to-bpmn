package com.dijkspicy.graphtobpmn.activity;

import com.dijkspicy.graphtobpmn.BaseActivity;

public class SetState extends BaseActivity {
    private final String state;

    public SetState(String obj, String state) {
        this.setId(obj + "-" + state);
        this.state = state;
    }

    public String getState() {
        return state;
    }
}