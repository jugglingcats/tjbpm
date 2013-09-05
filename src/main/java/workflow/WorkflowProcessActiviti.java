package workflow;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.parse.BpmnParseHandler;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import transform.Transformer;

import javax.xml.stream.XMLStreamException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class WorkflowProcessActiviti implements WorkflowProcess {
    public static final String OUT_ACTIVITI_BPMN = "out_activiti.bpmn";
    private static ProcessEngine processEngine;
    private final String processInstanceId;

    static {
        ProcessEngineConfigurationImpl configuration = (ProcessEngineConfigurationImpl) ProcessEngineConfiguration
                .createProcessEngineConfigurationFromResource("activiti.cfg.xml");
                // .createStandaloneInMemProcessEngineConfiguration();

        List<BpmnParseHandler> customListeners = new ArrayList<BpmnParseHandler>();
        customListeners.add(new ActivitiParseListener());
        configuration.setCustomDefaultBpmnParseHandlers(customListeners);

        processEngine = configuration.buildProcessEngine();
    }

    public static WorkflowProcessActiviti fromFSM(String source) throws FileNotFoundException {
        Transformer t = new Transformer(source, false);
        try {
            t.transform(OUT_ACTIVITI_BPMN);
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new WorkflowProcessActiviti(OUT_ACTIVITI_BPMN, Transformer.WORKFLOW_KEY);
    }

    public static void deploy(String sourcefile, String key) throws FileNotFoundException {
        InputStream ris = new FileInputStream(sourcefile);
        DeploymentBuilder builder = processEngine.getRepositoryService().createDeployment();
        builder.addInputStream(sourcefile, ris);
        builder.deploy();
    }

    public WorkflowProcessActiviti(String sourcefile, String key) throws FileNotFoundException {
        deploy(sourcefile, key);
        processInstanceId = processEngine.getRuntimeService().startProcessInstanceByKey(key).getId();
    }

    @Override
    public String getCurrentState() {
        List<String> activityIds = processEngine.getRuntimeService().getActiveActivityIds(processInstanceId);
        return activityIds.get(0);
    }

    @Override
    public void executeTransition(String transitionName) {
        processEngine.getRuntimeService().setVariable(processInstanceId, "transition", transitionName);
        List<Task> tasks = processEngine.getTaskService().createTaskQuery().executionId(processInstanceId).list();
        assert (tasks.size() == 1);
        processEngine.getTaskService().complete(tasks.get(0).getId());
    }

    @Override
    public boolean isEnded() {
        ProcessInstance instance = getProcessInstance();
        return instance == null || instance.isEnded();
    }

    public ProcessInstance getProcessInstance() {
        return processEngine.getRuntimeService()
                .createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
    }

    public String getSubProcessState() {
        Execution execution = processEngine.getRuntimeService()
                .createExecutionQuery()
                .parentId(processInstanceId)
                .singleResult();

        List<String> activityIds = processEngine.getRuntimeService().getActiveActivityIds(execution.getId());
        List<String> ids = processEngine.getRuntimeService().getActiveActivityIds(processInstanceId);
        assert(activityIds.size() == 1);
        return activityIds.get(0);
    }

    public static void findActivity(String activity) {
        List<Execution> executions = processEngine.getRuntimeService().createExecutionQuery().activityId(activity).list();
        assert(executions.size() == 1);
    }

    public static void shutdown() {
        processEngine.close();
    }
}
