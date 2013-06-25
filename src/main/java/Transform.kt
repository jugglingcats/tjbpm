package transform

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.dom.get
import kotlin.dom.parseXml
import kotlin.dom.attribute
import java.util.HashSet
import org.w3c.dom.Element

data class Transition(val name: String, val source: String, val target: String)

val Element.parent : Element get() = this.getParentNode() as Element

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

    val xml =
}