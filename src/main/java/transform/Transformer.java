package transform;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: akirkpatrick
 * Date: 25/07/13
 * Time: 15:47
 * To change this template use File | Settings | File Templates.
 */
public class Transformer {
    public static final String WORKFLOW_KEY = "workflow_fsm";
    private final String source;
    private HashSet<String> multiIn = new HashSet<String>();
    private HashSet<String> multiOut = new HashSet<String>();
    private boolean jbpmMode = false;

    public Transformer(String source, boolean jbpmMode) {
        this.source=source;
        this.jbpmMode = jbpmMode;
    }

    public void transform(String out) throws XMLStreamException, IOException {
        File f = new File(out);
        if (f.exists()) {
            f.delete();
        }

        FileWriter o = new FileWriter(f);
        final CustomXmlWriter writer = new CustomXmlWriter(o);

        try {
            Document doc = parseXml(source);
            HashSet<Transition> idx = new HashSet<Transition>();
            final HashSet<String> states = new HashSet<String>();

            final NodeList transitionNodes = doc.getElementsByTagName("transition");
            for (int n = 0; n < transitionNodes.getLength(); n++) {
                Node t = transitionNodes.item(n);
                String name = t.getAttributes().getNamedItem("name").getNodeValue();
                String source = t.getParentNode().getParentNode().getAttributes().getNamedItem("name").getNodeValue();
                String target = t.getAttributes().getNamedItem("target").getNodeValue();
                idx.add(new Transition(name, source, target));
                states.add(source);
                states.add(target);
            }

            if (true) {
                states.add("start--reserved");
                idx.add(new Transition("begin", "start--reserved", "pre-born"));
            }

            final Transition[] transitions = idx.toArray(new Transition[]{});
            Arrays.sort(transitions);

            writer.ns("vcms", "http://com.piksel/vcms/workflow");
            writer.ns("bpmn2", "http://www.omg.org/spec/BPMN/20100524/MODEL");
            writer.elem("definitions", new XmlFunc() {
                @Override
                public void process() throws XMLStreamException {
                    writer.set("xmlns", "http://www.omg.org/spec/BPMN/20100524/MODEL");
                    writer.set("targetNamespace", "http://www.omg.org/bpmn20");
                    buildProcess(writer, states, transitions);
                }
            });
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            o.close();
        }
    }

    private void buildProcess(final CustomXmlWriter writer, final HashSet<String> states, final Transition[] transitions) throws XMLStreamException {
        writer.elem("process", new XmlFunc() {
            @Override
            public void process() throws XMLStreamException {
                writer.set("id", WORKFLOW_KEY);
                writer.set("isExecutable", "true");
                writer.elem("property", new XmlFunc() {
                    @Override
                    public void process() throws XMLStreamException {
                        writer.set("id", "transition");
                    }
                });

                for (final String state : states) {
                    final StateType t = stateType(state, transitions);

                    if (jbpmMode) {
                        addIncomingGateway(state, t, writer, transitions);
                        buildStateNode(state, t, writer);
                        addOutgoingGateway(state, t, writer, transitions);
                    } else {
                        buildStateNode(state, t, writer);
                    }
                }

                buildSequenceFlows(writer, transitions);
            }
        });
    }

    private void buildSequenceFlows(final CustomXmlWriter writer, Transition[] transitions) throws XMLStreamException {
        for (final Transition trans : transitions) {
            writer.elem("sequenceFlow", new XmlFunc() {
                @Override
                public void process() throws XMLStreamException {
                    writer.set("id", trans.getName() + "-" + trans.getSource() + ".." + trans.getTarget());
                    if (jbpmMode) {
                        writer.set("name", trans.getName() + "-" + trans.getSource() + ".." + trans.getTarget());
                    }

                    if (multiOut.contains(trans.getSource()) && jbpmMode) {
                        writer.set("sourceRef", trans.getSource() + "..outgoing");
                    } else {
                        writer.set("sourceRef", trans.getSource());
                    }

                    if (multiIn.contains(trans.getTarget()) && jbpmMode) {
                        writer.set("targetRef", trans.getTarget() + "..incoming");
                    } else {
                        writer.set("targetRef", trans.getTarget());
                    }

                    if (!trans.getSource().equals("start--reserved")) {
                        writeConditionExpression(writer, trans);
                    } else {
                        writeConditionExpressionTrue(writer);
                    }
                }
            });
        }
    }

    private void writeConditionExpressionTrue(CustomXmlWriter writer) throws XMLStreamException {
        if (jbpmMode) {
            writer.elem("conditionExpression", "return true");
        } else {
            writer.elem("conditionExpression", "${true}");
        }
    }

    private void writeConditionExpression(final CustomXmlWriter writer, final Transition trans) throws XMLStreamException {
        if (jbpmMode) {
            writer.elem("conditionExpression", "return \"" + trans.getName() + "\".equals(transition)");
        } else {
            writer.elem("extensionElements", new XmlFunc() {
                @Override
                public void process() throws XMLStreamException {
                    writer.elem("vcms", "transition", new XmlFunc() {
                        @Override
                        public void process() throws XMLStreamException {
                            writer.set("transition", trans.getName());
                        }
                    });
                }
            });
//            writer.elem("conditionExpression", "${\"" + trans.getName() + "\".equals(transition)}");
        }
    }

    private void buildStateNode(final String state, final StateType t, final CustomXmlWriter writer) throws XMLStreamException {
        writer.elem(getElem(t), new XmlFunc() {
            @Override
            public void process() throws XMLStreamException {
                writer.set("id", state);
                writer.set("name", state);

                if (t != StateType.NORMAL) {
                    return;
                }

                if (jbpmMode) {
                    writer.elem("ioSpecification", new XmlFunc() {
                        @Override
                        public void process() throws XMLStreamException {
                            writer.elem("dataOutput", new XmlFunc() {
                                @Override
                                public void process() throws XMLStreamException {
                                    writer.set("id", state + "..result");
                                    writer.set("name", "transition");
                                }
                            });
                            writer.emptyElem("inputSet");
                            writer.elem("outputSet", new XmlFunc() {
                                @Override
                                public void process() throws XMLStreamException {
                                    writer.elem("dataOutputRefs", state + "..result");
                                }
                            });
                        }
                    });
                    writer.elem("dataOutputAssociation", new XmlFunc() {
                        @Override
                        public void process() throws XMLStreamException {
                            writer.elem("sourceRef", state + "..result");
                            writer.elem("targetRef", "transition");
                        }
                    });
                }
            }
        });
    }

    private String getElem(StateType t) {
        if (jbpmMode) {
            return t.toElemJbpm();
        } else {
            return t.toElemActiviti();
        }
    }

    private void addIncomingGateway(final String state, StateType t, final CustomXmlWriter writer, final Transition[] transitions) throws XMLStreamException {
        if (t == StateType.NORMAL || t == StateType.END_EVENT) {
            final Collection<String> incoming = filter(transitions, new FilterPredicate() {
                @Override
                public String match(Transition t) {
                    return state.equals(t.getTarget()) ? t.getSource() : null;
                }
            });

            if (incoming.size() == 1) {
                return;
            }

            multiIn.add(state);

            final String gateway = state + "..incoming";
            writer.elem("exclusiveGateway", new XmlFunc() {
                @Override
                public void process() throws XMLStreamException {
                    writer.set("id", gateway);
                    writer.set("name", gateway);
                    writer.set("gatewayDirection", "Converging");
                    for (final String s : incoming) {
                        writer.elem("incoming", s + "..outgoing");
                    }
                    writer.elem("outgoing", state);
                }
            });
            writer.elem("sequenceFlow", new XmlFunc() {
                @Override
                public void process() throws XMLStreamException {
                    writer.set("id", state + "..in");
                    writer.set("name", state + "..in");
                    writer.set("sourceRef", gateway);
                    writer.set("targetRef", state);
                }
            });
        }
    }

    private void addOutgoingGateway(final String state, StateType t, final CustomXmlWriter writer, final Transition[] transitions) throws XMLStreamException {
        if (t == StateType.NORMAL || t == StateType.START_EVENT) {
            final Collection<String> outgoing = filter(transitions, new FilterPredicate() {
                @Override
                public String match(Transition t) {
                    return state.equals(t.getSource()) ? t.getTarget() : null;
                }
            });

            if (outgoing.size() == 1) {
                return;
            }

            multiOut.add(state);

            final String gateway = state + "..outgoing";
            writer.elem("sequenceFlow", new XmlFunc() {
                @Override
                public void process() throws XMLStreamException {
                    writer.set("id", state + "..out");
                    writer.set("name", state + "..out");
                    writer.set("sourceRef", state);
                    writer.set("targetRef", gateway);
                }
            });

            writer.elem("exclusiveGateway", new XmlFunc() {
                @Override
                public void process() throws XMLStreamException {
                    writer.set("id", gateway);
                    writer.set("name", gateway);
                    writer.set("gatewayDirection", "Diverging");

                    writer.elem("incoming", state);

                    for (final String s : outgoing) {
                        writer.elem("outgoing", s + "..incoming");
                    }
                }
            });
        }
    }

    private Collection<String> filter(Transition[] transitions, FilterPredicate filterPredicate) {
        HashSet<String> results = new HashSet<String>();
        for (Transition t : transitions) {
            String s = filterPredicate.match(t);
            if (s != null) {
                results.add(s);
            }
        }
        return results;
    }

    private StateType stateType(String state, Transition[] transitions) {
        int inbound = 0;
        int outbound = 0;
        for (Transition t : transitions) {
            if (state.equals(t.getTarget())) inbound++;
            if (state.equals(t.getSource())) outbound++;
        }

        if (inbound == 0) {
            return StateType.START_EVENT;
        } else if (outbound == 0) {
            return StateType.END_EVENT;
        }
        return StateType.NORMAL;
    }

    private Document parseXml(String s) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        return factory.newDocumentBuilder().parse(s);
    }
}
