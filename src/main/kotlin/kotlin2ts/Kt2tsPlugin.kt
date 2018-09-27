package kotlin2ts

import kotlin2ts.internal.Kt2tsExtension
import kotlin2ts.internal.Kt2tsTask
import org.gradle.api.Plugin
import org.gradle.api.Project

open class Kt2tsPlugin : Plugin<Project> {

    override fun apply(project: Project) = project.run {
        extensions.create(Kt2tsExtension.name, Kt2tsExtension::class.java)

        afterEvaluate {
            println("Hello Plugin \\o/")
        }

        tasks.create(Kt2tsTask.defaultName, Kt2tsTask::class.java) {
            it.group = "Development"
            it.description = "Get the lines of code for files"
        }
        Unit
    }

}