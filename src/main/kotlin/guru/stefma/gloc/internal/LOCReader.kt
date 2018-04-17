package guru.stefma.gloc.internal

import java.io.File

/**
 * Read all files **this** directory and map extensions to the count the line numbers of it.
 *
 * @param recursive true if we read the directory recursive. false will only read **this** directory.
 * @throws IllegalStateException if **this** file is not a directory.
 */
fun File.readFileExtensionsWithLinesInDir(recursive: Boolean = true): Map<String, Int> {
    if (!isDirectory) throw IllegalStateException("This file ('$this') should be directory not a file!")

    val mutableMap = mutableMapOf<String, Int>()
    return writeExtensionAndLinesInMap(mutableMap, recursive).run { mutableMap }
}

private fun File.writeExtensionAndLinesInMap(mutableMap: MutableMap<String, Int>, recursive: Boolean) {

    listFiles().forEach {
        if (it.isDirectory) {
            if (recursive) it.writeExtensionAndLinesInMap(mutableMap, recursive)
        } else {
            val loc = mutableMap[it.extension]
            mutableMap[it.extension] = loc?.plus(it.readLines().size) ?: it.readLines().size
        }
    }

}
