package kotlin2ts.internal

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import java.io.File

/**
 * the task is intended to be configured only via plugin's extension (see [Kt2tsExtension])
 */
@CacheableTask
open class Kt2tsTask : DefaultTask() {

    @OutputFile
    val output: File = project.file("${project.buildDir}/gloc/gloc.txt")

    @Nested // for now it is not possible to put the annotation above dirsWithContent directly
    var boilerplate = emptyList<DirWithContent>()
        get() = dirsWithContent

    private val ext = project.extensions.findByName(Kt2tsExtension.name) as Kt2tsExtension

    private val dirsWithContent by lazy { checkConfiguredDirs().map { DirWithContent(it) } }

    /**
     * @throws IllegalStateException if either no input directory is configured or configured input is not a directory
     * @return list of configured directories to get LOC statistics from
     */
    private fun checkConfiguredDirs(): List<File> {
        val dirs = ext.dirs
        val enabled = ext.enabled
        check(dirs.isNotEmpty() || !enabled) { "gloc.dirs should be set!" }

        return dirs.map {
            File(it).apply {
                check(isDirectory) { "$it input should be directory!" }
            }
        }
    }

    @TaskAction
    fun action() {
        if (!ext.enabled) return
        createOutputFile()
        cleanOutputFile()
        dirsWithContent.forEach { appendToOutput(it) }
        println("Output can be found at ${output.absolutePath}")
    }

    private fun createOutputFile() = output.run {
        if (!parentFile.exists()) {
            parentFile.mkdirs()
        }
        if (!exists()) {
            createNewFile()
        }
    }

    private fun cleanOutputFile() = output.writeText("")

    private fun appendToOutput(input: DirWithContent) = output.run {
        appendText("Directory '${input.dir.name}':")
        appendText("\n")
        input.readLoc().forEach { key, value ->
            appendText("'$key' has '$value' LOC in sum")
            appendText("\n")
        }
    }

    companion object {
        const val defaultName = "gloc"
    }

}

private val sum = { x: Int, y: Int -> x + y }

/**
 * the class serves two purposes:
 * a) tracks changes inside configured dir (see @Input and @InputFiles annotated [dir] and [content] properties)
 * b) provides statistics for LOC in dir with [readLoc]
 */
data class DirWithContent(@Input val dir: File) {
    /**
     * all files in directory, recursively
     */
    @InputFiles
    val content = dir.walkTopDown().asSequence().filter { it.isFile }.toList()

    /**
     * provide Lines of Code stats for directory content
     */
    fun readLoc(): Map<String, Int> = content.fold(mutableMapOf()) { map, file ->
        map.apply {
            merge(file.extension, file.readLines().size, sum)
        }
    }
}

