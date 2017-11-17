package com.dijkspicy.graphtobpmn.activity;

import com.dijkspicy.graphtobpmn.BaseActivity;

/**
 * graph-to-bpmn
 *
 * @Author dijkspicy
 * @Date 2017/11/17
 */
public abstract class ExecutableActivity<T> extends BaseActivity {
    protected final T target;

    protected ExecutableActivity(String id, T target) {
        super(id);
        this.target = target;
    }

    public T getTarget() {
        return target;
    }
}
