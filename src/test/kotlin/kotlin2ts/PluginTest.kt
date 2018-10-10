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

@ExtendWith(TemporaryDir::class)
class PluginTest {

    private fun File.runGradleTask(useCache: Boolean = false): BuildResult {
        val args = mutableListOf("kt2ts")
        if (useCache) {
            args += "--build-cache"
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

    private fun BuildResult.assertOutcome(outcome: TaskOutcome) {
        assertThat(task(":kt2ts")!!.outcome).isEqualTo(outcome)
    }

    private fun File.build_gradle(vararg packs: String) {
        resolve("build.gradle").run {
            val confPacks = packs.asSequence().joinToString(",", "\"", "\"")
            writeText(
                    """
                        plugins {
                            id("kotlin2ts")
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

    private fun File.assertOutputContains(vararg chunks: String) = readTaskOutput().run {
        chunks.forEach { assertThat(this).contains(it) }
    }

    @Test
    fun `task should read test sample file and should write generated data to output`(tempDir: File) = tempDir.run {
//        tempDir.copyInto("/kotlin2ts/games/cards/Cards.kt")
        build_gradle("kotlin2ts.games.cards")

        runGradleTask()
        assertOutputContains("Player", "Rarity", "Inventory", "Card")
    }

    @Test
    fun `apply run task twice - should be up to date`(tempDir: File) = tempDir.run {
//        tempDir.copyInto("/kotlin2ts/games/cards/Cards.kt")
        build_gradle("kotlin2ts.games.cards")

        runGradleTask().assertOutcome(SUCCESS)
        runGradleTask().assertOutcome(UP_TO_DATE)
    }

}