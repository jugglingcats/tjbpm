package transform

import javax.xml.stream.XMLStreamWriter
import java.util.HashMap

class XmlWriter(val xml: XMLStreamWriter) {
    val nstable = HashMap<String, String>()
    var nsprefix: String = "";

    fun elem(tag: String, init: XmlWriter.() -> Unit) {
        writeStartElement(tag)
        init()
        xml.writeEndElement()
    }

    fun set(attr: String, value: String) {
        xml.writeAttribute(attr, value)
    }

    fun text(text: String) {
        xml.writeCharacters(text)
    }

    private fun writeStartElement(tag: String) {
        if ( !nsprefix.equals("") ) {
            val uri = nstable[nsprefix]
            if ( xml.getPrefix(uri) == null ) {
                xml.setPrefix(nsprefix, uri)
                xml.writeStartElement(uri, tag)
                xml.writeNamespace(nsprefix, uri)
            } else {
                xml.writeStartElement(uri, tag)
            }
        } else {
            xml.writeStartElement(tag)
        }
    }
    fun ns(prefix: String, namespace: String) {
        nsprefix = prefix
        if ( nstable.containsKey(prefix) ) {
            return
        }
        nstable[prefix] = namespace
    }
}

