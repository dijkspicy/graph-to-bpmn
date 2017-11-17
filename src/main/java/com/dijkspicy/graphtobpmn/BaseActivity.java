package com.dijkspicy.graphtobpmn;

public abstract class BaseActivity {
    private final String id;
    private String name;

    protected BaseActivity(String id) {
        this.id = id;
        this.name = id;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
