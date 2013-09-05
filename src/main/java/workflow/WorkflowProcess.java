package workflow;

/**
 * Created with IntelliJ IDEA.
 * User: akirkpatrick
 * Date: 01/08/13
 * Time: 14:02
 * To change this template use File | Settings | File Templates.
 */
public interface WorkflowProcess {
    String getCurrentState();
    void executeTransition(String transitionName);
    boolean isEnded();
}
