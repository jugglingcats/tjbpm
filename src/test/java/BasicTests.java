import org.drools.KnowledgeBase;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.event.DefaultProcessEventListener;
import org.drools.event.process.ProcessEventListener;
import org.drools.event.process.ProcessNodeTriggeredEvent;
import org.drools.io.ResourceFactory;
import org.drools.process.instance.WorkItemHandler;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.process.*;
import org.jbpm.ruleflow.instance.RuleFlowProcessInstance;
import org.junit.Test;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: alfie
 * Date: 05/06/13
 * Time: 22:09
 * To change this template use File | Settings | File Templates.
 */
public class BasicTests extends DefaultProcessEventListener {

    @Test
    public void test() {
        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        kbuilder.add(ResourceFactory.newClassPathResource("test.bpmn"), ResourceType.BPMN2);
        KnowledgeBase kbase = kbuilder.newKnowledgeBase();

        StatefulKnowledgeSession ksession = kbase.newStatefulKnowledgeSession();

//        ksession.getWorkItemManager().registerWorkItemHandler("Human Task", new WSHumanTaskHandler());
        ksession.getWorkItemManager().registerWorkItemHandler("Manual Task", new WorkItemHandler() {
            @Override
            public void executeWorkItem(WorkItem workItem, WorkItemManager workItemManager) {
                System.out.println("Work item name: "+workItem.getName()+", id: "+workItem.getId());
            }

            @Override
            public void abortWorkItem(WorkItem workItem, WorkItemManager workItemManager) {
            }
        });

        ksession.addEventListener(this);

        ProcessInstance processInstance = ksession.startProcess("test");
        ksession.getWorkItemManager().completeWorkItem(1, null);
        NodeInstanceContainer container= (NodeInstanceContainer) processInstance;
        for ( NodeInstance s : container.getNodeInstances() ) {
            System.out.println("Active node: "+s.getNodeName()+", id:" +s.getNodeId());
        }


        System.out.println("Process state: "+processInstance.getState());
    }

    @Override
    public void beforeNodeTriggered(ProcessNodeTriggeredEvent event) {
        System.out.println("Before node: "+event.getNodeInstance().getNodeName());
    }
}
