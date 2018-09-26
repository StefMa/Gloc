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

    @Test
    fun `apply and enabled true - should print hello plugin`(tempDir: File) {
        File(tempDir, "build.gradle.kts").run {
            writeText(
                    """
                        plugins {
                            id("kotlin2ts")
                        }
                        """
            )
        }

        val buildResult = runner(tempDir).build()
        assertThat(buildResult.output).contains("Hello Plugin \\o/")
    }

    @Test
    fun `apply enabled without dirs - should fail with exception`(tempDir: File) {
        File(tempDir, "build.gradle").run {
            writeText(
                    """
                        plugins {
                            id "kotlin2ts"
                        }

                        kt2ts {
                            enabled = true
                        }
                        """
            )
        }

        val failResult = runner(tempDir, "kt2ts").buildAndFail()
        assert(failResult.output.contains("kt2ts.dirs should be set!"))
    }

    @Test
    fun `apply and enabled false should not print hello plugin`(tempDir: File) {
        File(tempDir, "build.gradle").run {
            writeText(
                    """
                        plugins {
                            id "kotlin2ts"
                        }

                        kt2ts {
                            enabled = false
                        }
                        """
            )
        }

        val buildResult = runner(tempDir).build()
        assertThat(buildResult.output).doesNotContain("Hello Plugin \\o/")
    }

    @Test
    fun `apply disabled run task - should have a empty text in build dir`(tempDir: File) {
        File(tempDir, "build.gradle").run {
            writeText(
                    """
                        plugins {
                            id "kotlin2ts"
                        }

                        kt2ts {
                            enabled = false
                        }
                        """
            )
        }

        runner(tempDir, "kt2ts").build()
        val kt2tsFileText = File(tempDir, "build/kt2ts/kt2ts.txt")
        assert(!kt2tsFileText.exists())
    }

    @Test
    fun `apply run task twice - should be up to date`(tempDir: File) {
        File(tempDir, "build.gradle").run {
            writeText(
                    """
                        plugins {
                            id "kotlin2ts"
                        }

                        kt2ts {
                            enabled = false
                        }
                        """
            )
        }

        val result = runner(tempDir, "kt2ts").build()
        val resultUpToDate = runner(tempDir, "kt2ts").build()

        assertThat(result.task(":kt2ts")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(resultUpToDate.task(":kt2ts")!!.outcome ).isEqualTo(TaskOutcome.UP_TO_DATE)
    }

    @Test
    fun `task should read test xml file and should write loc in it`(tempDir: File) {
        File(tempDir, "build.gradle").run {
            writeText(
                    """
                        plugins {
                            id "kotlin2ts"
                        }

                        kt2ts {
                            enabled = true
                            dirs = [projectDir.path + "/source"]
                        }
                        """
            )
        }
        File(tempDir, "source/test.xml").apply {
            parentFile.mkdirs()
            createNewFile()
            writeText("This \n is \n droidcon \n italy \n turin")
        }

        runner(tempDir, "kt2ts").build()
        val kt2tsFileText = File(tempDir, "build/kt2ts/kt2ts.txt")
        assertThat(kt2tsFileText.readText()).contains("5")
    }

    @Test
    fun `task should run again after test file was modified`(tempDir: File) {
        File(tempDir, "build.gradle").run {
            writeText(
                    """
                        plugins {
                            id "kotlin2ts"
                        }

                        kt2ts {
                            enabled = true
                            dirs = [projectDir.path + "/source"]
                        }
                        """
            )
        }
        val testXml = File(tempDir, "source/test.xml").apply {
            parentFile.mkdirs()
            createNewFile()
            writeText("This \n is \n droidcon \n italy \n turin")
        }

        val result = runner(tempDir, "kt2ts").build()
        assertThat(result.task(":kt2ts")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        val kt2tsFileText = File(tempDir, "build/kt2ts/kt2ts.txt")
        assertThat(kt2tsFileText.readText()).contains("5")

        // updating same file
        testXml.appendText("\nta\nda-a-am")

        val result2 = runner(tempDir, "kt2ts").build()
        assertThat(result2.task(":kt2ts")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(kt2tsFileText.readText()).contains("7")
    }

    @Test
    fun `task should read recursive files and should write loc in it`(tempDir: File) {
        File(tempDir, "build.gradle").run {
            writeText(
                    """
                        plugins {
                            id "kotlin2ts"
                        }

                        kt2ts {
                            enabled = true
                            dirs = [projectDir.path + "/source"]
                        }
                        """
            )
        }
        File(tempDir, "source/test.xml").apply {
            parentFile.mkdirs()
            createNewFile()
            writeText("This\nis\na\nnew\nfile")
        }
        File(tempDir, "source/another/test.xml").apply {
            parentFile.mkdirs()
            createNewFile()
            writeText("Another\nfile\nwith\nnew\nlines")
        }

        runner(tempDir, "kt2ts").build()
        val kt2tsFileText = File(tempDir, "build/kt2ts/kt2ts.txt")
        assert(kt2tsFileText.readText().contains("10"))
    }

    @Test
    fun `task should read recursive files and multiple dirs should write loc in it`(tempDir: File) {
        File(tempDir, "build.gradle").run {
            writeText(
                    """
                        plugins {
                            id "kotlin2ts"
                        }

                        kt2ts {
                            enabled = true
                            dirs = [projectDir.path + "/source", projectDir.path + "/notSource"]
                        }
                        """
            )
        }
        File(tempDir, "source/test.xml").apply {
            parentFile.mkdirs()
            createNewFile()
            writeText("This\nis\na\nnew\nfile")
        }
        File(tempDir, "source/another/test.xml").apply {
            parentFile.mkdirs()
            createNewFile()
            writeText("Another\nfile\nwith\nnew\nlines")
        }
        File(tempDir, "notSource/test.xml").apply {
            parentFile.mkdirs()
            createNewFile()
            writeText("Awesome\nnew\nlines")
        }

        runner(tempDir, "kt2ts").build()
        val kt2tsFileText = File(tempDir, "build/kt2ts/kt2ts.txt")
        assert(kt2tsFileText.readText().contains("10"))
        assert(kt2tsFileText.readText().contains("3"))
    }

    @Test
    fun `task should read from build cache`(tempDir: File) {
        File(tempDir, "build.gradle").run {
            writeText(
                    """
                        plugins {
                            id "kotlin2ts"
                        }

                        kt2ts {
                            enabled = true
                            dirs = [projectDir.path + "/source"]
                        }
                        """
            )
        }
        File(tempDir, "source/test.xml").apply {
            parentFile.mkdirs()
            createNewFile()
            writeText("This\nis\na\nnew\nfile")
        }

        File(tempDir, "settings.gradle").run {
            writeText(
                    """
                        buildCache {
                            local { directory = "$tempDir/cache" }
                        }
                    """
            )
        }

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
        File(tempDir, "build.gradle").run {
            writeText(
                    """
                        plugins {
                            id "kotlin2ts"
                        }

                        kt2ts {
                            enabled = true
                            dirs = [projectDir.path + "/source", projectDir.path + "/notSource"]
                        }
                        """
            )
        }
        File(tempDir, "source/test.xml").apply {
            parentFile.mkdirs()
            createNewFile()
            writeText("This\nis\na\nnew\nfile")
        }
        File(tempDir, "source/another/test.html").apply {
            parentFile.mkdirs()
            createNewFile()
            writeText("Another\nfile\nwith\n6\nnew\nlines")
        }
        File(tempDir, "notSource/test.kt").apply {
            parentFile.mkdirs()
            createNewFile()
            writeText("Awesome\nnew\nlines")
        }

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
        File(tempDir, "build.gradle").run {
            writeText(
                    """
                        plugins {
                            id "kotlin2ts"
                        }

                        kt2ts {
                            enabled = true
                            dirs = [projectDir.path + "/$path"]
                        }
                        """
            )
        }
        File(tempDir, "$path/test.xml").apply {
            parentFile.mkdirs()
            createNewFile()
            writeText("This\nis\na\nnew\nfile")
        }

        val buildResult = runner(tempDir, "kt2ts").build()
        val kt2tsFileText = File(tempDir, "build/kt2ts/kt2ts.txt")
        assert(kt2tsFileText.readText().contains("5"))
        println(buildResult.output)
        stuff(buildResult)
    }

}