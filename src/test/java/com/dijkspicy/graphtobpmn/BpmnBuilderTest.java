package com.dijkspicy.graphtobpmn;

import com.dijkspicy.graphtobpmn.activity.CallOperation;
import com.dijkspicy.graphtobpmn.activity.SetState;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.ServiceTask;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

class BpmnBuilderTest {
    @Test
    void create() throws IOException {

        MutableGraph<BaseActivity> graph = GraphBuilder.directed().build();

        Object target = new Object();
        BaseActivity a_ing = new SetState<>("a_ing", target, "ing");
        BaseActivity a_call = new CallOperation<>("a_call", target, "create");
        BaseActivity a_ed = new SetState<>("a_ed", target, "ed");
        graph.putEdge(a_ing, a_call);
        graph.putEdge(a_call, a_ed);

        // children of a
        BaseActivity b_ing = new SetState<>("b_ing", target, "ing");
        BaseActivity b_call = new CallOperation<>("b_call", target, "create");
        BaseActivity b_ed = new SetState<>("b_ed", target, "ed");
        graph.putEdge(b_ing, b_call);
        graph.putEdge(b_call, b_ed);

        BaseActivity c_ing = new SetState<>("c_ing", target, "ing");
        BaseActivity c_call = new CallOperation<>("c_call", target, "create");
        BaseActivity c_ed = new SetState<>("c_ed", target, "ed");
        graph.putEdge(c_ing, c_call);
        graph.putEdge(c_call, c_ed);

        // children of c, and e depends on d
        BaseActivity d_ing = new SetState<>("d_ing", target, "ing");
        BaseActivity d_call = new CallOperation<>("d_call", target, "create");
        BaseActivity d_ed = new SetState<>("d_ed", target, "ed");
        graph.putEdge(d_ing, d_call);
        graph.putEdge(d_call, d_ed);

        BaseActivity e_ing = new SetState<>("e_ing", target, "ing");
        BaseActivity e_call = new CallOperation<>("e_call", target, "create");
        BaseActivity e_ed = new SetState<>("e_ed", target, "ed");
        graph.putEdge(e_ing, e_call);
        graph.putEdge(e_call, e_ed);

        // composition: source ing then target ing
        graph.putEdge(a_ing, b_ing);
        graph.putEdge(a_ing, c_ing);
        graph.putEdge(c_ing, d_ing);
        graph.putEdge(c_ing, e_ing);
        // composition: target ed then source call
        graph.putEdge(b_ed, a_call);
        graph.putEdge(c_ed, a_call);
        graph.putEdge(d_ed, c_call);
        graph.putEdge(e_ed, c_call);
        // depends on: target ed then source call
        graph.putEdge(d_ed, e_call);

        BpmnModel bpmnModel = new BpmnBuilder<Object>(graph) {
            @Override
            protected void complete(ServiceTask serviceTask, SetState<Object> baseActivity) {

            }

            @Override
            protected void complete(ServiceTask serviceTask, CallOperation<Object> baseActivity) {

            }
        }.buildWithDI("test");
        BpmnXMLConverter converter = new BpmnXMLConverter();
        byte[] xmlBytes = converter.convertToXML(bpmnModel);
        Files.write(Paths.get("create-workflow.xml"), xmlBytes);
    }
}