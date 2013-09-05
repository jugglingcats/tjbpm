package workflow;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;

/**
 * Created with IntelliJ IDEA.
 * User: akirkpatrick
 * Date: 04/09/13
 * Time: 16:29
 * To change this template use File | Settings | File Templates.
 */
public class ActivitiWaitService implements JavaDelegate {
    @Override
    public void execute(DelegateExecution execution) throws Exception {
        Thread.sleep(300);
    }
}
