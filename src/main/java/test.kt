package test

class Writer {
    fun something(v:String) {}
    fun set(n:String, v:String) {}
}

fun writer(init:Writer.() -> Unit) {
    val w=Writer()
    w.init()
}

fun test() {
    writer {
        something("hello")

        // aren't these three equivalent?
        this["name"]="value"
        set("name", "value")
//        ["name"]="value"
    }
}