package kotlin2ts

import kotlin2ts.extension.TemporaryDir
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File
import java.io.FileOutputStream
import java.util.Objects.requireNonNull

@ExtendWith(TemporaryDir::class)
class PluginTest {

    private val testKotlinVersion = "1.2.71"

    private enum class Task(val gradleName: String, val classpath: String? = null) {
        COMPILE("compileKotlin"),
        KT2TS("kt2ts", "build/classes/kotlin/main/")
    }

    private fun File.runGradleTask(task: Task, useCache: Boolean = false): BuildResult {
        val args = mutableListOf(task.gradleName)
        if (useCache) {
            args += "--build-cache"
        }
        return GradleRunner.create()
                .withProjectDir(this)
                .withPluginClasspath()
                .withArguments(*args.toTypedArray())
                .withDebug(true)
                .apply {
                    task.classpath?.let {
                        withPluginClasspath(
                                mutableListOf<File>().apply {
                                    addAll(pluginClasspath)
                                    add(resolve(it))
                                }
                        )
                    }
                }
                .build()
    }

    private fun File.copyInto(name: String) {
        resolve("src/main/kotlin")
                .apply {
                    if (!exists()) mkdirs()
                }
                .resolve(File(name).name)
                .run {
                    FileOutputStream(this)
                }
                .use { os ->
                    requireNonNull(
                            javaClass.getResourceAsStream(name),
                            "$name not found in classpath"
                    ).use { it.copyTo(os) }
                }
    }

    private fun BuildResult.assertOutcome(task: Task, outcome: TaskOutcome) {
        assertThat(task(":${task.gradleName}")!!.outcome).isEqualTo(outcome)
    }

    private fun File.build_gradle(vararg packs: String) {
        resolve("build.gradle").run {
            val confPacks = packs.asSequence().joinToString(",", "\"", "\"")
            writeText(
                    """
                        plugins {
                            id("org.jetbrains.kotlin.jvm").version("$testKotlinVersion")
                            id("kotlin2ts")
                        }

                        repositories {
                            jcenter()
                        }

                        dependencies {
                            implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
                        }

                        kt2ts {
                            packs = [$confPacks]
                        }
                        """
            )
        }
    }

    private fun File.settings_gradle() = resolve("settings.gradle.kts").writeText(
            """
                        buildCache {
                            local { directory = "$this/cache" }
                        }
                 """
    )

    private fun File.readTaskOutput() = resolve("build/kt2ts/kt2ts.txt").readText()

    @Test
    fun `task should read test sample file and should write generated data to output`(tempDir: File): Unit = tempDir.run {
        tempDir.copyInto("/kotlin2ts/games/cards/Cards.kt")
        build_gradle("kotlin2ts.games.cards")

        runGradleTask(Task.COMPILE)
        runGradleTask(Task.KT2TS)

        val output = readTaskOutput()
        assertThat(output).contains("Player")
        assertThat(output).contains("Rarity")
        assertThat(output).contains("Inventory")
        assertThat(output).contains("Card")
    }

    @Test
    fun `apply run task twice - should be up to date`(tempDir: File) = tempDir.run {
        tempDir.copyInto("/kotlin2ts/games/cards/Cards.kt")
        build_gradle("kotlin2ts.games.cards")

        runGradleTask(Task.COMPILE).assertOutcome(Task.COMPILE, SUCCESS)
        runGradleTask(Task.KT2TS).assertOutcome(Task.KT2TS, SUCCESS)
        runGradleTask(Task.KT2TS).assertOutcome(Task.KT2TS, UP_TO_DATE)
    }

}