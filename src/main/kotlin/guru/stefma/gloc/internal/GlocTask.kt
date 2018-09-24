package guru.stefma.gloc.internal

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

private const val outputFile = "gloc/gloc.txt"
private const val inputDirsFile = "gloc/inputdirs.txt"

@CacheableTask
open class GlocTask : DefaultTask() {

    @OutputFile
    val output = project.file("${project.buildDir}/$outputFile")

    @InputFile
    var inputDirs = project.file("${project.buildDir}/$inputDirsFile")

    @TaskAction
    fun action() {
        val extension = project.extensions.run {
            findByName(GlocExtension.name) as GlocExtension
        }

        if (extension.enabled) {
            createPrettyOutputFile(extension.dirs)
            println("Output can be found at ${output.absolutePath}")
        }
    }

    private fun createPrettyOutputFile(dirs: Array<String>) {
        output.createNewFile()
        output.writeText("")

        if (dirs.isEmpty())
            throw IllegalArgumentException("gloc.dirs should be set!")

        dirs.countLinesOrderedByExtension()
                .forEach { (dir, map) ->
                    output.appendText("Directory '${File(dir).name}':")
                    output.appendText("\n")
                    map.forEach {
                        output.appendText("'${it.key}' has '${it.value}' LOC in sum")
                        output.appendText("\n")
                    }
                }
    }

    companion object {
        const val defaultName = "gloc"
    }

}

/**
 * A simple helper task to create a stable input file for the [GlocTask].
 */
open class GlocInputTask : DefaultTask() {

    @TaskAction
    fun writeInput() {
        val inputFile = project.file("${project.buildDir}/$inputDirsFile").apply {
            parentFile.mkdirs()
            createNewFile()
            writeText("")
        }

        val input = (project.extensions.findByName(GlocExtension.name) as GlocExtension).dirs
        input.forEach { inputFile.appendText(it) }
    }

}

/**
 * Read all files for each given dirPath and sort by extension and count the line of code of them.
 */
private fun Array<String>.countLinesOrderedByExtension(): List<Pair<String, Map<String, Int>>> {
    return map { dirPath -> dirPath to File(dirPath).readFileExtensionsWithLinesInDir() }
}