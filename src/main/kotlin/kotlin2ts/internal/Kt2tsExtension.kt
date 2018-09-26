package kotlin2ts.internal

open class Kt2tsExtension {

    var enabled: Boolean = true

    var packs: Array<String> = emptyArray()

    companion object {
        const val name = "kt2ts"
    }

}