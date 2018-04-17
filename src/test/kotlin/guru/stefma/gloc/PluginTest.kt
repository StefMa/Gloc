package guru.stefma.gloc

import guru.stefma.gloc.extension.TemporaryDir
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File

@ExtendWith(TemporaryDir::class)
class PluginTest {

    @Test
    fun `apply and enabled true - should print hello plugin`(tempDir: File) {
        File(tempDir, "build.gradle.kts").run {
            writeText(
                    """
                        plugins {
                            id("guru.stefma.gloc")
                        }
                        """
            )
        }

        val buildResult = GradleRunner.create()
                .withProjectDir(tempDir)
                .withPluginClasspath()
                .build()

        assertThat(buildResult.output).contains("Hello Plugin \\o/")
    }

    @Test
    fun `apply enabled without dirs - should fail with exception`(tempDir: File) {
        File(tempDir, "build.gradle").run {
            writeText(
                    """
                        plugins {
                            id "guru.stefma.gloc"
                        }

                        gloc {
                            enabled = true
                        }
                        """
            )
        }

        val failResult = GradleRunner.create()
                .withProjectDir(tempDir)
                .withPluginClasspath()
                .withArguments("gloc")
                .buildAndFail()

        assert(failResult.output.contains("gloc.dirs should be set!"))
    }

    @Test
    fun `apply and enabled false should not print hello plugin`(tempDir: File) {
        File(tempDir, "build.gradle").run {
            writeText(
                    """
                        plugins {
                            id "guru.stefma.gloc"
                        }

                        gloc {
                            enabled = false
                        }
                        """
            )
        }

        val buildResult = GradleRunner.create()
                .withProjectDir(tempDir)
                .withPluginClasspath()
                .build()

        assertThat(buildResult.output).doesNotContain("Hello Plugin \\o/")
    }

    @Test
    fun `apply disabled run task - should have a empty text in build dir`(tempDir: File) {
        File(tempDir, "build.gradle").run {
            writeText(
                    """
                        plugins {
                            id "guru.stefma.gloc"
                        }

                        gloc {
                            enabled = false
                        }
                        """
            )
        }

        GradleRunner.create()
                .withProjectDir(tempDir)
                .withPluginClasspath()
                .withArguments("gloc")
                .build()

        val glocFileText = File(tempDir, "build/gloc/gloc.txt")
        assert(!glocFileText.exists())
    }

    @Test
    fun `apply run task twice - should be up to date`(tempDir: File) {
        File(tempDir, "build.gradle").run {
            writeText(
                    """
                        plugins {
                            id "guru.stefma.gloc"
                        }

                        gloc {
                            enabled = false
                        }
                        """
            )
        }

        val result = GradleRunner.create()
                .withProjectDir(tempDir)
                .withPluginClasspath()
                .withArguments("gloc")
                .build()

        val resultUpToDate = GradleRunner.create()
                .withProjectDir(tempDir)
                .withPluginClasspath()
                .withArguments("gloc")
                .build()

        assertThat(result.task(":gloc")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(resultUpToDate.task(":gloc")!!.outcome ).isEqualTo(TaskOutcome.UP_TO_DATE)
    }

    @Test
    fun `task should read test xml file and should write loc in it`(tempDir: File) {
        File(tempDir, "build.gradle").run {
            writeText(
                    """
                        plugins {
                            id "guru.stefma.gloc"
                        }

                        gloc {
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

        GradleRunner.create()
                .withProjectDir(tempDir)
                .withPluginClasspath()
                .withArguments("gloc")
                .build()

        val glocFileText = File(tempDir, "build/gloc/gloc.txt")
        assertThat(glocFileText.readText()).contains("5")
    }

    @Test
    fun `task should read recursive files and should write loc in it`(tempDir: File) {
        File(tempDir, "build.gradle").run {
            writeText(
                    """
                        plugins {
                            id "guru.stefma.gloc"
                        }

                        gloc {
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

        GradleRunner.create()
                .withProjectDir(tempDir)
                .withPluginClasspath()
                .withArguments("gloc")
                .build()

        val glocFileText = File(tempDir, "build/gloc/gloc.txt")
        assert(glocFileText.readText().contains("10"))
    }

    @Test
    fun `task should read recursive files and multiple dirs should write loc in it`(tempDir: File) {
        File(tempDir, "build.gradle").run {
            writeText(
                    """
                        plugins {
                            id "guru.stefma.gloc"
                        }

                        gloc {
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

        GradleRunner.create()
                .withProjectDir(tempDir)
                .withPluginClasspath()
                .withArguments("gloc")
                .build()

        val glocFileText = File(tempDir, "build/gloc/gloc.txt")
        assert(glocFileText.readText().contains("10"))
        assert(glocFileText.readText().contains("3"))
    }

    @Test
    fun `task should read from build cache`(tempDir: File) {
        File(tempDir, "build.gradle").run {
            writeText(
                    """
                        plugins {
                            id "guru.stefma.gloc"
                        }

                        gloc {
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

        val build = GradleRunner.create()
                .withProjectDir(tempDir)
                .withPluginClasspath()
                .withArguments("gloc", "--build-cache")
                .build()
        assertThat(build.task(":gloc")!!.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)

        // Clean build dir and run again - should be read from build cache
        File(tempDir, "build/").deleteRecursively()
        val build2 = GradleRunner.create()
                .withProjectDir(tempDir)
                .withPluginClasspath()
                .withArguments("gloc", "--build-cache")
                .build()

        assertThat(build2.task(":gloc")!!.outcome)
                .isEqualTo(TaskOutcome.FROM_CACHE)
    }

    @Test
    fun `task should read recursive files and multiple dirs should write loc in it2`(tempDir: File) {
        File(tempDir, "build.gradle").run {
            writeText(
                    """
                        plugins {
                            id "guru.stefma.gloc"
                        }

                        gloc {
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

        GradleRunner.create()
                .withProjectDir(tempDir)
                .withPluginClasspath()
                .withArguments("gloc")
                .build()

        val glocFileText = File(tempDir, "build/gloc/gloc.txt")
        println(glocFileText.readText())
        assert(glocFileText.readText().contains("5"))
        assert(glocFileText.readText().contains("6"))
        assert(glocFileText.readText().contains("3"))
    }

    @Test
    fun `task run twice with different dirs should be run twice`(tempDir: File) {
        runTest(tempDir, "source") {
            assertThat(it.task(":gloc")!!.outcome)
                    .isEqualTo(TaskOutcome.SUCCESS)
        }
        runTest(tempDir, "anotherSource") {
            assertThat(it.task(":gloc")!!.outcome)
                    .isEqualTo(TaskOutcome.SUCCESS)
        }
    }

    private fun runTest(tempDir: File, path: String, stuff: (result: BuildResult) -> Unit) {
        File(tempDir, "build.gradle").run {
            writeText(
                    """
                        plugins {
                            id "guru.stefma.gloc"
                        }

                        gloc {
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

        val buildResult = GradleRunner.create()
                .withProjectDir(tempDir)
                .withPluginClasspath()
                .withArguments("gloc")
                .build()

        val glocFileText = File(tempDir, "build/gloc/gloc.txt")
        assert(glocFileText.readText().contains("5"))
        println(buildResult.output)
        stuff(buildResult)
    }

}