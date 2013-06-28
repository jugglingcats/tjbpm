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
import java.io.FileWriter
import java.io.File

data class Transition(val name: String, val source: String, val target: String)

val Element.parent: Element get() = this.getParentNode() as Element

open class XmlWriter(val xml: XMLStreamWriter) {
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

fun writer(out: Writer, init: XmlWriter.() -> Unit) {
    val xml = IndentingXMLStreamWriter(XMLOutputFactory.newInstance()!!.createXMLStreamWriter(out)!!)
    val writer = XmlWriter(xml)
    xml.writeStartDocument()
    writer.init()
    xml.writeEndDocument()
    xml.close()
}

fun main(args: Array<String>) {
    val doc = parseXml("workflow_sample.xml")

    val idx: HashSet<Transition> = HashSet<Transition>()
    val states = HashSet<String>()

    doc.get("transition").forEach {
        val name = it.attribute("name")
        val source = it.parent.parent.attribute("name")
        val target = it.attribute("target")
        idx.add(Transition(name, source, target))
        states.add(source)
        states.add(target)
    }

    val transitions = idx.sortBy { it -> "${it.name}/${it.source}/${it.target}" }
    transitions.forEach {
        println("name: ${it.name}, source: ${it.source}, target: ${it.target}")
    }

    val f = File("out.bpmn")
    if ( f.exists() ) {
        f.delete()
    }
    val o = FileWriter(f)
    val xml = writer(o) {
        ns("bpmn2", "http://www.omg.org/spec/BPMN/20100524/MODEL")
        elem("definitions") {
            set("xmlns", "http://www.omg.org/bpmn20")
            set("targetNamespace", "http://www.omg.org/bpmn20")
            elem("process") {
                set("id", "test")
                set("isExecutable", "true")
                states.forEach {
                    val state = it
                    val elem = elemType(it, transitions)
                    when (elem) {
                        "endEvent", "manualTask" -> {
                            val gateway = "$it..incoming"
                            elem("exclusiveGateway") {
                                set("id", gateway)
                                set("gatewayDirection", "Converging")
                                val list = transitions.filter { it.target == state } map { it.source }
                                list.distinct() forEach {
                                    elem("incoming") { text("$it..outgoing") }
                                }
                                elem("outgoing") {
                                    text(it)
                                }
                            }
                            elem("sequenceFlow") {
                                set("id", "$it..in")
                                set("sourceRef", gateway)
                                set("targetRef", it)
                            }
                        }
                        else -> {
                        }
                    }

                    elem(elem) {
                        set("id", it)
                        set("name", it)
                    }

                    when (elem) {
                        "startEvent", "manualTask" -> {
                            val gateway = "$it..outgoing"
                            elem("sequenceFlow") {
                                set("id", "$it..out")
                                set("sourceRef", it)
                                set("targetRef", gateway)
                            }
                            elem("exclusiveGateway") {
                                set("id", gateway)
                                set("gatewayDirection", "Diverging")
                                elem("incoming") { text(it) }
                                val list = transitions.filter { it.source == state } map { it.target }
                                list.distinct() forEach {
                                    elem("outgoing") { text("$it..incoming") }
                                }
                            }
                        }
                        else -> {
                        }
                    }
                }
                transitions.forEach {
                    elem("sequenceFlow") {
                        set("id", "${it.name}-${it.source}..${it.target}")
                        set("sourceRef", "${it.source}..outgoing")
                        set("targetRef", "${it.target}..incoming")
                    }
                }
            }
        }
    }
    println(o.toString());
}

fun <T> Collection<T>.distinct() : Collection<T> {
    val ret=HashSet<T>()
    this.forEach { ret.add(it) }
    return ret;
}

fun elemType(state: String, transitions: List<Transition>): String {
    return if ( transitions.count { state == it.target } == 0 ) {
        "startEvent"
    } else if ( transitions.count { state == it.source } == 0 ) {
        "endEvent"
    } else {
        "manualTask"
    }
}