package workflow;

import org.activiti.bpmn.model.ExtensionAttribute;
import org.activiti.bpmn.model.ExtensionElement;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.engine.impl.Condition;
import org.activiti.engine.impl.bpmn.parser.BpmnParse;
import org.activiti.engine.impl.bpmn.parser.handler.SequenceFlowParseHandler;
import org.activiti.engine.impl.el.UelExpressionCondition;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.impl.pvm.process.ScopeImpl;
import org.activiti.engine.impl.pvm.process.TransitionImpl;

import java.util.Map;

public class ActivitiParseListener extends SequenceFlowParseHandler {

    @Override
    protected void executeParse(BpmnParse bpmnParse, SequenceFlow sequenceFlow) {
        // let base class create the transition
        super.executeParse(bpmnParse, sequenceFlow);

        // do we have any extension elements?
        Map<String,ExtensionElement> elementMap = sequenceFlow.getExtensionElements();
        if ( elementMap.containsKey("transition") ) {
            ExtensionElement extensionElement = elementMap.get("transition");
            ExtensionAttribute extensionAttribute = extensionElement.getAttributes().get("transition");
            String name = extensionAttribute.getValue();

            // build the conditional expression
            String expression=String.format("${\"%s\".equals(transition)}", name);
            Condition expressionCondition = new UelExpressionCondition(bpmnParse.getExpressionManager().createExpression(expression));

            // find the source activity and existing transition (added by base class)
            ScopeImpl scope = bpmnParse.getCurrentScope();
            ActivityImpl sourceActivity = scope.findActivity(sequenceFlow.getSourceRef());
            TransitionImpl transition = sourceActivity.findOutgoingTransition(sequenceFlow.getId());

            // overwrite any existing condition expression
            transition.setProperty(PROPERTYNAME_CONDITION_TEXT, expression);
            transition.setProperty(PROPERTYNAME_CONDITION, expressionCondition);
        }
    }
}
