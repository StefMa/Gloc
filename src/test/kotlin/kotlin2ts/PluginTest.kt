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

    private fun runner(tempDir: File, vararg args: String): GradleRunner {
        return GradleRunner.create()
                .withProjectDir(tempDir)
                .withPluginClasspath()
                .apply {
                    if (args.isNotEmpty()) {
                        withArguments(*args)
                    }
                }
                .withDebug(true)
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


    @Test
    fun `apply run task twice - should be up to date`(tempDir: File) {
        tempDir.build_gradle("source")
        tempDir.src("source/test.xml", "This \n is \n droidcon \n italy \n turin")

        val result = runner(tempDir, "kt2ts").build()
        val resultUpToDate = runner(tempDir, "kt2ts").build()

        assertThat(result.task(":kt2ts")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(resultUpToDate.task(":kt2ts")!!.outcome ).isEqualTo(TaskOutcome.UP_TO_DATE)
    }

    @Test
    fun `task should read test xml file and should write loc in it`(tempDir: File) {
        tempDir.build_gradle("source")
        tempDir.src("source/test.xml", "This \n is \n droidcon \n italy \n turin")

        runner(tempDir, "kt2ts").build()
        val kt2tsFileText = File(tempDir, "build/kt2ts/kt2ts.txt")
        assertThat(kt2tsFileText.readText()).contains("5")
    }

    @Test
    fun `task should run again after test file was modified`(tempDir: File) {
        tempDir.build_gradle("source")
        tempDir.src("source/test.xml", "This \n is \n droidcon \n italy \n turin")

        val result = runner(tempDir, "kt2ts").build()
        assertThat(result.task(":kt2ts")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        val kt2tsFileText = File(tempDir, "build/kt2ts/kt2ts.txt")
        assertThat(kt2tsFileText.readText()).contains("5")

        // updating same file
        tempDir.src("source/test.xml", "This \n is \n droidcon \n italy \n turin\nta\nda-a-am")

        val result2 = runner(tempDir, "kt2ts").build()
        assertThat(result2.task(":kt2ts")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(kt2tsFileText.readText()).contains("7")
    }

    @Test
    fun `task should read recursive files and should write loc in it`(tempDir: File) {
        tempDir.build_gradle("source")
        tempDir.src("source/test.xml", "This\nis\na\nnew\nfile")
        tempDir.src("source/another/test.xml", "Another\nfile\nwith\nnew\nlines")

        runner(tempDir, "kt2ts").build()
        val kt2tsFileText = File(tempDir, "build/kt2ts/kt2ts.txt")
        assert(kt2tsFileText.readText().contains("10"))
    }

    @Test
    fun `task should read recursive files and multiple dirs should write loc in it`(tempDir: File) {
        tempDir.build_gradle("source", "notSource")
        tempDir.src("source/test.xml", "This\nis\na\nnew\nfile")
        tempDir.src("source/another/test.xml", "Another\nfile\nwith\nnew\nlines")
        tempDir.src("notSource/test.xml", "Awesome\nnew\nlines")

        runner(tempDir, "kt2ts").build()
        val kt2tsFileText = File(tempDir, "build/kt2ts/kt2ts.txt")
        assert(kt2tsFileText.readText().contains("10"))
        assert(kt2tsFileText.readText().contains("3"))
    }

    @Test
    fun `task should read from build cache`(tempDir: File) {
        tempDir.build_gradle("source")
        tempDir.src("source/test.xml", "This\nis\na\nnew\nfile")
        tempDir.settings_gradle()

        val build = runner(tempDir, "kt2ts", "--build-cache").build()
        assertThat(build.task(":kt2ts")!!.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)

        // Clean build dir and run again - should be read from build cache
        File(tempDir, "build/").deleteRecursively()
        val build2 = runner(tempDir, "kt2ts", "--build-cache").build()
        assertThat(build2.task(":kt2ts")!!.outcome)
                .isEqualTo(TaskOutcome.FROM_CACHE)
    }

    @Test
    fun `task should read recursive files and multiple dirs should write loc in it2`(tempDir: File) {
        tempDir.build_gradle("source", "notSource")
        tempDir.src("source/test.xml", "This\nis\na\nnew\nfile")
        tempDir.src("source/another/test.html", "Another\nfile\nwith\n6\nnew\nlines")
        tempDir.src("notSource/test.kt", "Awesome\nnew\nlines")

        runner(tempDir, "kt2ts").build()
        val kt2tsFileText = File(tempDir, "build/kt2ts/kt2ts.txt")
        println(kt2tsFileText.readText())
        assert(kt2tsFileText.readText().contains("5"))
        assert(kt2tsFileText.readText().contains("6"))
        assert(kt2tsFileText.readText().contains("3"))
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

        val buildResult = runner(tempDir, "kt2ts").build()
        val kt2tsFileText = File(tempDir, "build/kt2ts/kt2ts.txt")
        assert(kt2tsFileText.readText().contains("5"))
        println(buildResult.output)
        stuff(buildResult)
    }

}