package kotlin2ts

import kotlin2ts.internal.Kt2tsExtension
import kotlin2ts.internal.Kt2tsTask
import org.gradle.api.Plugin
import org.gradle.api.Project

open class Kt2tsPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.run {
            create(Kt2tsExtension.name, Kt2tsExtension::class.java)
        }

        project.afterEvaluate {
            if (!extension.enabled) return@afterEvaluate
            println("Hello Plugin \\o/")
        }

        project.tasks.create(Kt2tsTask.defaultName, Kt2tsTask::class.java) {
            it.group = "Development"
            it.description = "Get the lines of code for files"
        }
    }

}