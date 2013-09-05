package workflow;

import org.drools.KnowledgeBase;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderConfiguration;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.io.Resource;
import org.drools.io.ResourceFactory;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.process.*;
import transform.Transformer;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class WorkflowProcessJbpm implements WorkItemHandler, WorkflowProcess {
    public static final String OUT_JBPM_BPMN = "out_jbpm.bpmn";
    private ProcessInstance processInstance;
    private long currentWorkItem=-1;
    private final StatefulKnowledgeSession ksession;

    public static WorkflowProcessJbpm fromFSM(String source) {
        Transformer t=new Transformer(source, true);
        try {
            t.transform(OUT_JBPM_BPMN);
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new WorkflowProcessJbpm(OUT_JBPM_BPMN, Transformer.WORKFLOW_KEY);
    }

    public WorkflowProcessJbpm(String sourcefile, String key) {
        KnowledgeBuilderConfiguration kbc = KnowledgeBuilderFactory.newKnowledgeBuilderConfiguration();
        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder(kbc);

        Resource resource = ResourceFactory.newFileResource(sourcefile);

        kbuilder.add(resource, ResourceType.BPMN2);
        KnowledgeBase kbase = kbuilder.newKnowledgeBase();

        ksession = kbase.newStatefulKnowledgeSession();
        ksession.getWorkItemManager().registerWorkItemHandler("Manual Task", this);
        processInstance = ksession.startProcess(key);
    }

    @Override
    public String getCurrentState() {
        NodeInstanceContainer container = (NodeInstanceContainer) processInstance;
        Collection<NodeInstance> nodes = container.getNodeInstances();
        if ( nodes.size() > 1 ) {
            throw new RuntimeException("Process has more than one active node - not supported at this time!");
        }
        for (NodeInstance s : nodes) {
            return s.getNodeName();
        }
        throw new RuntimeException("Process has no active nodes!");
    }

    @Override
    public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
        if ( currentWorkItem >= 0 ) {
            throw new RuntimeException("Existing work item was not completed correctly!");
        }
        currentWorkItem=workItem.getId();
    }

    @Override
    public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
    }

    @Override
    public void executeTransition(String transitionName) {
        if ( currentWorkItem < 0 ) {
            throw new RuntimeException("There is no current work item for transition: "+transitionName);
        }

        Map<String, Object> results = new HashMap<String, Object>();
        results.put("transition", transitionName);
        long id=currentWorkItem;
        currentWorkItem=-1;
        ksession.getWorkItemManager().completeWorkItem(id, results);
    }

    @Override
    public boolean isEnded() {
        return processInstance.getState() == ProcessInstance.STATE_COMPLETED;
    }
}
