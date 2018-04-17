package guru.stefma.gloc

import guru.stefma.gloc.internal.GlocExtension
import guru.stefma.gloc.internal.GlocInputTask
import guru.stefma.gloc.internal.GlocTask
import org.gradle.api.Plugin
import org.gradle.api.Project

open class GlocPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.run {
            create("gloc", GlocExtension::class.java)
        }

        project.afterEvaluate {
            if (!extension.enabled) return@afterEvaluate
            println("Hello Plugin \\o/")
        }

        val inputTask = project.tasks.run {
            create("glocInput", GlocInputTask::class.java)
        }

        with(project.tasks) {
            create("gloc", GlocTask::class.java) {
                it.group = "Development"
                it.description = "Get the lines of code for files"
                it.dependsOn(inputTask)
            }
        }
    }

}