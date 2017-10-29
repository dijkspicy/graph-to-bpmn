package com.tttx.demo.gtob;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import org.activiti.bpmn.BpmnAutoLayout;
import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.BpmnModel;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.TemporalField;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class BpmnBuilderTest {
    @Test
    void create() throws IOException {

        MutableGraph<BaseActivity> graph = GraphBuilder.directed().build();

        BaseActivity a_ing = new SetState("a", "ing");
        BaseActivity a_call = new CallOperation("a", "create");
        BaseActivity a_ed = new SetState("a", "ed");
        graph.putEdge(a_ing, a_call);
        graph.putEdge(a_call, a_ed);

        // children of a
        BaseActivity b_ing = new SetState("b", "ing");
        BaseActivity b_call = new CallOperation("b", "create");
        BaseActivity b_ed = new SetState("b", "ed");
        graph.putEdge(b_ing, b_call);
        graph.putEdge(b_call, b_ed);

        BaseActivity c_ing = new SetState("c", "ing");
        BaseActivity c_call = new CallOperation("c", "create");
        BaseActivity c_ed = new SetState("c", "ed");
        graph.putEdge(c_ing, c_call);
        graph.putEdge(c_call, c_ed);

        // children of c, and e depends on d
        BaseActivity d_ing = new SetState("d", "ing");
        BaseActivity d_call = new CallOperation("d", "create");
        BaseActivity d_ed = new SetState("d", "ed");
        graph.putEdge(d_ing, d_call);
        graph.putEdge(d_call, d_ed);

        BaseActivity e_ing = new SetState("e", "ing");
        BaseActivity e_call = new CallOperation("e", "create");
        BaseActivity e_ed = new SetState("e", "ed");
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

        List<Long> times = new ArrayList<>();
        for(int i = 0; i < 100; i++) {
            long start = System.currentTimeMillis();
            BpmnModel bpmnModel = new BpmnBuilder(graph).build("test");
            new BpmnAutoLayout(bpmnModel).execute();
            BpmnXMLConverter converter = new BpmnXMLConverter();
            byte[] xmlBytes = converter.convertToXML(bpmnModel);
            long now = System.currentTimeMillis();
            long sub = now - start;
            times.add(sub);
        }
        int total = 0;
        for (Long time : times) {
            total += time;
        }
        System.out.println(total);
        System.out.println(total/times.size());

//        Files.write(Paths.get("create-workflow.xml"), xmlBytes);
    }
}