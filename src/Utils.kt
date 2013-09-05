package transform

import java.util.HashSet
import org.w3c.dom.Element

val Element.parent: Element get() = this.getParentNode() as Element

fun <T> Collection<T>.distinct() : Collection<T> {
    val ret=HashSet<T>()
    this.forEach { ret.add(it) }
    return ret;
}

