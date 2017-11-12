package com.dijkspicy.graphtobpmn;

public abstract class BaseActivity {
    private String id;
    private String type;

    public String getId() {
        return id;
    }

    public BaseActivity setId(String id) {
        this.id = id;

        return this;
    }

    public String getType() {
        return type;
    }

    public BaseActivity setType(String type) {
        this.type = type;
        return this;
    }
}
