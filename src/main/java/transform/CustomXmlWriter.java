package transform;

import com.sun.xml.internal.txw2.output.IndentingXMLStreamWriter;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.FileWriter;
import java.util.HashMap;

/**
 * Helper class to build properly balanced XML output
 */
public class CustomXmlWriter {
    private HashMap<String, String> nstable=new HashMap<String, String>();
    private String nsprefix;
    private XMLStreamWriter xml;

    public CustomXmlWriter(FileWriter o) throws XMLStreamException {
        xml=new IndentingXMLStreamWriter(XMLOutputFactory.newInstance().createXMLStreamWriter(o));
    }

    public void ns(String prefix, String uri) throws XMLStreamException {
        nsprefix=prefix;
        if ( nstable.containsKey(prefix) ) {
            return;
        }
        nstable.put(prefix, uri);
    }

    // element with nested content using currently active namespace
    public void elem(String name, XmlFunc fn) throws XMLStreamException {
        writeStartElement(name);
        fn.process();
        writeEndElement();
    }

    // element with nested content using specified namespace (by prefix)
    public void elem(String prefix, String name, XmlFunc fn) throws XMLStreamException {
        String currentPrefix=nsprefix;
        nsprefix=prefix;
        writeStartElement(name);
        fn.process();
        writeEndElement();
        nsprefix=currentPrefix;
    }

    private void writeEndElement() throws XMLStreamException {
        xml.writeEndElement();
    }

    private void writeStartElement(String tag) throws XMLStreamException {
        if ( !nsprefix.equals("") ) {
            String uri = nstable.get(nsprefix);
            if ( xml.getPrefix(uri) == null ) {
                xml.setPrefix(nsprefix, uri);
                xml.writeStartElement(uri, tag);
                xml.writeNamespace(nsprefix, uri);
            } else {
                xml.writeStartElement(uri, tag);
            }
        } else {
            xml.writeStartElement(tag);
        }
    }

    // write an attribute
    public void set(String attr, String value) throws XMLStreamException {
        xml.writeAttribute(attr, value);
    }

    // write some text content
    public void text(String s) throws XMLStreamException {
        xml.writeCharacters(s);
    }

    // write an empty element (no attributes)
    public void emptyElem(String tag) throws XMLStreamException {
        elem(tag, new XmlFunc() {
            @Override
            public void process() throws XMLStreamException {
            }
        });
    }

    // write a simple element with text content (no attributes)
    public void elem(String tag, final String innerText) throws XMLStreamException {
        elem(tag, new XmlFunc() {
            @Override
            public void process() throws XMLStreamException {
                text(innerText);
            }
        });
    }
}
