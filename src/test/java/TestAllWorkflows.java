import junit.framework.Assert;
import org.junit.AfterClass;
import org.junit.Test;
import workflow.WorkflowProcess;
import workflow.WorkflowProcessActiviti;
import workflow.WorkflowProcessJbpm;

import java.io.FileNotFoundException;

public class TestAllWorkflows extends Assert {
    @AfterClass
    public static void shutdown() {
        WorkflowProcessActiviti.shutdown();
    }

    @Test
    public void mainJbpm() {
        WorkflowProcess process=WorkflowProcessJbpm.fromFSM("workflow_sample.xml");

        assertEquals("pre-born", process.getCurrentState());
        process.executeTransition("new");
        assertEquals("created", process.getCurrentState());
    }

    @Test
    public void mainActiviti() throws FileNotFoundException {
        WorkflowProcess process=WorkflowProcessActiviti.fromFSM("workflow_sample.xml");

        assertEquals("pre-born", process.getCurrentState());
        process.executeTransition("new");
        assertEquals("created", process.getCurrentState());
    }

    @Test
    public void subprocess() throws FileNotFoundException, InterruptedException {
        // in this test the main process invokes a sub-process which contains a long running service task
        // (marked as async)

        // deploy the sub-process but don't start it
        WorkflowProcessActiviti.deploy("src/test/resources/sub-process.bpmn", "sub-process");

        // start the main process
        WorkflowProcessActiviti workflowMain=new WorkflowProcessActiviti("src/test/resources/test.bpmn", "test");
        assertEquals("saved", workflowMain.getCurrentState());

        // proceed on main process
        workflowMain.executeTransition("approve");
        // main process starts sub-process and waits
        assertEquals("invokejob", workflowMain.getCurrentState());

        // wait for sub-process to complete
        Thread.sleep(500);

        // main process has moved on
        assertEquals("published", workflowMain.getCurrentState());
    }
}
