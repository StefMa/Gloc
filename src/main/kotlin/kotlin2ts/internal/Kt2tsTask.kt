@file:Suppress("UnstableApiUsage")

// ^^^ consider OK Guava class reflection

package kotlin2ts.internal

import com.google.common.reflect.ClassPath
import com.google.common.reflect.ClassPath.ClassInfo
import me.ntrrgc.tsGenerator.TypeScriptGenerator
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.reflect.KClass

/**
 * the task is intended to be configured only via plugin's extension (see [Kt2tsExtension])
 */
@CacheableTask
open class Kt2tsTask : DefaultTask() {

    @OutputFile
    val output: File = project.file("${project.buildDir}/kt2ts/kt2ts.txt")

    @Nested // for now it is not possible to put the annotation above packsWithClasses directly
    var boilerplate = emptyList<PacksWithClasses>()
        get() = packsWithClasses

    private val ext = project.extensions.findByName(Kt2tsExtension.name) as Kt2tsExtension

    private val packsWithClasses by lazy { ext.packs.map { PacksWithClasses(it) } }

    @TaskAction
    fun action() {
        maybeCreateOutputDir()
        writeToOutputFile(
                packsWithClasses.asSequence()
                        .flatMap(PacksWithClasses::load)
                        .toSet()
        )
        println("Output can be found at ${output.absolutePath}")
    }

    private fun maybeCreateOutputDir() = output.run {
        if (!parentFile.exists()) {
            parentFile.mkdirs()
        }
    }

    private fun writeToOutputFile(input: Iterable<KClass<*>>) = output.run {
        writeText(
                TypeScriptGenerator(
                        rootClasses = input,
                        mappings = mapOf(
                                LocalDateTime::class to "Date",
                                LocalDate::class to "Date"
                        )
                ).definitionsText
        )
    }

    companion object {
        const val defaultName = "kt2ts"
    }

}

/**
 * the class serves two purposes:
 * a) tracks ABI's changes of classes inside specified pack (see @CompileClasspath annotated [compileClassPath] property)
 * b) load classes on demand [load]
 */
data class PacksWithClasses(@Input val pack: String) {
    /**
     * all load in package
     */
    val content: Set<ClassInfo> = ClassPath.from(ClassLoader.getSystemClassLoader()).getTopLevelClassesRecursive(pack)

    @CompileClasspath
    val compileClassPath = content.map { it.resourceName }

    /**
     * provide loaded classes
     */
    fun load(): Sequence<KClass<*>> = content.asSequence().map { it.load() }.map { it.kotlin }
}

