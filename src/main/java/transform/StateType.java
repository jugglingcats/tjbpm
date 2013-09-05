package transform;

/**
 * There is a difference between jBPM and Activiti, in that jBPM supports manualTask as a wait state,
 * but doesn't support userTask ootb. Activiti just treats manualTask as a passthrough and so
 * prefers userTask for wait states
 */
public enum StateType {
    START_EVENT,
    END_EVENT,
    NORMAL;

    public String toElemJbpm() {
        switch (this) {
            case START_EVENT:
                return "startEvent";
            case END_EVENT:
                return "endEvent";
            default:
                return "manualTask";
        }
    }

    public String toElemActiviti() {
        switch (this) {
            case START_EVENT:
                return "startEvent";
            case END_EVENT:
                return "endEvent";
            default:
                return "userTask";
        }
    }
}
