package kotlin2ts.internal

open class Kt2tsExtension {

    var enabled: Boolean = true

    var dirs: Array<String> = emptyArray()

    companion object {
        const val name = "gloc"
    }

}