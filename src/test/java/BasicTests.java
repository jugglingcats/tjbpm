import org.drools.KnowledgeBase;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderConfiguration;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.event.DefaultProcessEventListener;
import org.drools.event.process.ProcessNodeTriggeredEvent;
import org.drools.io.ResourceFactory;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.process.*;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: alfie
 * Date: 05/06/13
 * Time: 22:09
 * To change this template use File | Settings | File Templates.
 */
public class BasicTests extends DefaultProcessEventListener implements org.drools.runtime.process.WorkItemHandler {

    @Test
    public void test() {
        KnowledgeBuilderConfiguration kbc = KnowledgeBuilderFactory.newKnowledgeBuilderConfiguration();
        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder(kbc);

        kbuilder.add(ResourceFactory.newClassPathResource("test.bpmn"), ResourceType.BPMN2);
        KnowledgeBase kbase = kbuilder.newKnowledgeBase();

        StatefulKnowledgeSession ksession = kbase.newStatefulKnowledgeSession();

        ksession.getWorkItemManager().registerWorkItemHandler("Manual Task", this);
        ksession.addEventListener(this);

        ProcessInstance processInstance = ksession.startProcess("test");

        System.out.println("Completing work item with id: 1");
        Map<String, Object> results = new HashMap<String, Object>();
        results.put("transition", "reject");
        ksession.getWorkItemManager().completeWorkItem(1, results);

        // output where we ended up
        NodeInstanceContainer container = (NodeInstanceContainer) processInstance;
        for (NodeInstance s : container.getNodeInstances()) {
            System.out.println("Active node: " + s.getNodeName() + ", id:" + s.getNodeId());
        }

        System.out.println("Process state: " + processInstance.getState());
    }

    @Override
    public void beforeNodeTriggered(ProcessNodeTriggeredEvent event) {
        System.out.println("Before node: " + event.getNodeInstance().getNodeName());
    }

    @Override
    public void executeWorkItem(WorkItem workItem, WorkItemManager workItemManager) {
        System.out.println("Work item name: " + workItem.getName() + ", id: " + workItem.getId());
    }

    @Override
    public void abortWorkItem(WorkItem workItem, WorkItemManager workItemManager) {
    }
}
