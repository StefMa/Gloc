package kotlin2ts

import kotlin2ts.extension.TemporaryDir
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File

@ExtendWith(TemporaryDir::class)
class PluginTest {

    private fun File.runGradleBuild(useCache : Boolean = false): BuildResult {
        val args = mutableListOf("kt2ts")
        if (useCache) {
            args.add("--build-cache")
        }
        return GradleRunner.create()
                .withProjectDir(this)
                .withPluginClasspath()
                .apply {
                    if (args.isNotEmpty()) {
                        withArguments(*args.toTypedArray())
                    }
                }
                .withDebug(true)
                .build()
    }

    private fun File.build_gradle(vararg packs: String) {
        resolve("build.gradle").run {
            val confPacks = packs.asSequence().map { "projectDir.path + \"/$it\"" }.joinToString()
            writeText(
                    """
                        plugins {
                            id "kotlin2ts"
                        }

                        kt2ts {
                            packs = [$confPacks]
                        }
                        """
            )
        }
    }

    private fun File.settings_gradle() = run {
        resolve("settings.gradle").writeText(
                """
                        buildCache {
                            local { directory = "$this/cache" }
                        }
                    """)
    }

    private fun File.src(path: String, content: String) {
        resolve(path).run {
            if (!parentFile.exists()) {
                parentFile.mkdirs()
            }
            writeText(content)
        }
    }

    private fun File.readTaskOutput() = resolve("build/kt2ts/kt2ts.txt").readText()

    private fun File.assertOutputContains(vararg chunks: String) = readTaskOutput().run {
        chunks.forEach { assertThat(this).contains(it) }
    }

    @Test
    fun `apply run task twice - should be up to date`(tempDir: File) {
        tempDir.build_gradle("source")
        tempDir.src("source/test.xml", "This \n is \n droidcon \n italy \n turin")

        val result = tempDir.runGradleBuild()
        val resultUpToDate = tempDir.runGradleBuild()

        assertThat(result.task(":kt2ts")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(resultUpToDate.task(":kt2ts")!!.outcome ).isEqualTo(TaskOutcome.UP_TO_DATE)
    }

    @Test
    fun `task should read test xml file and should write loc in it`(tempDir: File) {
        tempDir.build_gradle("source")
        tempDir.src("source/test.xml", "This \n is \n droidcon \n italy \n turin")

        tempDir.runGradleBuild()
        tempDir.assertOutputContains("5")
    }

    @Test
    fun `task should run again after test file was modified`(tempDir: File) {
        tempDir.build_gradle("source")
        tempDir.src("source/test.xml", "This \n is \n droidcon \n italy \n turin")

        val result = tempDir.runGradleBuild()
        assertThat(result.task(":kt2ts")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        tempDir.assertOutputContains("5")

        // updating same file
        tempDir.src("source/test.xml", "This \n is \n droidcon \n italy \n turin\nta\nda-a-am")

        val result2 = tempDir.runGradleBuild()
        assertThat(result2.task(":kt2ts")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        tempDir.assertOutputContains("7")
    }

    @Test
    fun `task should read recursive files and should write loc in it`(tempDir: File) {
        tempDir.build_gradle("source")
        tempDir.src("source/test.xml", "This\nis\na\nnew\nfile")
        tempDir.src("source/another/test.xml", "Another\nfile\nwith\nnew\nlines")

        tempDir.runGradleBuild()
        tempDir.assertOutputContains("10")
    }

    @Test
    fun `task should read recursive files and multiple dirs should write loc in it`(tempDir: File) {
        tempDir.build_gradle("source", "notSource")
        tempDir.src("source/test.xml", "This\nis\na\nnew\nfile")
        tempDir.src("source/another/test.xml", "Another\nfile\nwith\nnew\nlines")
        tempDir.src("notSource/test.xml", "Awesome\nnew\nlines")

        tempDir.runGradleBuild()
        tempDir.assertOutputContains("10", "3")
    }

    @Test
    fun `task should read from build cache`(tempDir: File) {
        tempDir.build_gradle("source")
        tempDir.src("source/test.xml", "This\nis\na\nnew\nfile")
        tempDir.settings_gradle()

        val build = tempDir.runGradleBuild(true)
        assertThat(build.task(":kt2ts")!!.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)

        // Clean build dir and run again - should be read from build cache
        File(tempDir, "build/").deleteRecursively()
        val build2 = tempDir.runGradleBuild(true)
        assertThat(build2.task(":kt2ts")!!.outcome)
                .isEqualTo(TaskOutcome.FROM_CACHE)
    }

    @Test
    fun `task should read recursive files and multiple dirs should write loc in it2`(tempDir: File) {
        tempDir.build_gradle("source", "notSource")
        tempDir.src("source/test.xml", "This\nis\na\nnew\nfile")
        tempDir.src("source/another/test.html", "Another\nfile\nwith\n6\nnew\nlines")
        tempDir.src("notSource/test.kt", "Awesome\nnew\nlines")

        tempDir.runGradleBuild()
        tempDir.assertOutputContains("5", "6", "3")
    }

    @Test
    fun `task run twice with different dirs should be run twice`(tempDir: File) {
        runTest(tempDir, "source") {
            assertThat(it.task(":kt2ts")!!.outcome)
                    .isEqualTo(TaskOutcome.SUCCESS)
        }
        runTest(tempDir, "anotherSource") {
            assertThat(it.task(":kt2ts")!!.outcome)
                    .isEqualTo(TaskOutcome.SUCCESS)
        }
    }

    private fun runTest(tempDir: File, path: String, stuff: (result: BuildResult) -> Unit) {
        tempDir.build_gradle(path)
        tempDir.src("$path/test.xml", "This\nis\na\nnew\nfile")

        val buildResult = tempDir.runGradleBuild()
        tempDir.assertOutputContains("5")
        println(buildResult.output)
        stuff(buildResult)
    }

}