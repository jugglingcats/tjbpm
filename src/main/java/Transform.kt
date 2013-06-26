package transform

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.dom.get
import kotlin.dom.parseXml
import kotlin.dom.attribute
import java.util.HashSet
import org.w3c.dom.Element
import javax.xml.stream.XMLOutputFactory
import java.io.OutputStream
import java.io.Writer
import java.io.StringWriter
import javax.xml.stream.XMLStreamWriter
import com.sun.xml.internal.txw2.output.IndentingXMLStreamWriter
import java.util.HashMap

data class Transition(val name: String, val source: String, val target: String)

val Element.parent : Element get() = this.getParentNode() as Element

open class XmlWriter(val xml: XMLStreamWriter) {
    val nstable=HashMap<String, String>()
    var nsprefix:String="";

    fun elem(tag: String, init: XmlWriter.() -> Unit) {
        writeStartElement(tag)
        init()
        xml.writeEndElement()
    }

    private fun writeStartElement(tag:String) {
        if ( !nsprefix.equals("") ) {
            val uri=nstable[nsprefix]
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
    fun ns(prefix:String, namespace:String) {
        nsprefix=prefix
        if ( nstable.containsKey(prefix) ) {
            return
        }
        nstable[prefix]=namespace
    }
}

fun writer(out:Writer, init : XmlWriter.() -> Unit) {
    val xml = IndentingXMLStreamWriter(XMLOutputFactory.newInstance()!!.createXMLStreamWriter(out)!!)
    val writer=XmlWriter(xml)
    xml.writeStartDocument()
    writer.init()
    xml.writeEndDocument()
    xml.close()
}

fun main(args: Array<String>) {
    val doc = parseXml("workflow_sample.xml")

    val idx:HashSet<Transition> = HashSet<Transition>()

    doc.get("transition").forEach {
        idx.add(Transition(it.attribute("name"), it.parent.parent.attribute("name"), it.attribute("target")))
    }

    val sorted=idx.sortBy { it -> "${it.name}/${it.source}/${it.target}" }
    sorted.forEach {
        println("name: ${it.name}, source: ${it.source}, target: ${it.target}")
    }

    val o=StringWriter()
    val xml = writer(o) {
        ns("bpmn2", "http://www.omg.org/bpmn20")
        elem("process") {
            sorted.forEach {
                elem("abc") {}
            }
        }
    }
    println(o.toString());
}