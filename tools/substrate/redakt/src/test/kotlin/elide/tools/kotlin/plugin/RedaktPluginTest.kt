@file:OptIn(org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi::class)
@file:Suppress("DEPRECATION")

package elide.tools.kotlin.plugin

import com.google.common.truth.Truth.assertThat
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.PluginOption
import com.tschuchort.compiletesting.SourceFile
import elide.tools.kotlin.plugin.redakt.KEY_ANNOTATION
import elide.tools.kotlin.plugin.redakt.KEY_ENABLED
import elide.tools.kotlin.plugin.redakt.KEY_MASK
import elide.tools.kotlin.plugin.redakt.RedaktRegistrar
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/** Top-level tests for the Redakt plugin. */
@Disabled class RedaktPluginTest : AbstractKotlinPluginTest() {
  companion object {
    const val testPackage = "elide.tools.kotlin.plugin.redakt.test"
  }

  private val sensitiveAnnoSrc = SourceFile.kotlin("Sensitive.kt", """
    package $testPackage
    import kotlin.annotation.AnnotationRetention.BINARY
    import kotlin.annotation.AnnotationTarget.PROPERTY
    import kotlin.annotation.AnnotationTarget.CLASS

    @Retention(BINARY)
    @Target(PROPERTY, CLASS)
    annotation class Sensitive
  """.trimIndent())

  /** @inheritDoc */
  override fun cliProcessor(): CommandLineProcessor = RedaktPlugin()

  /** @inheritDoc */
  override fun defaultOptions(processor: CommandLineProcessor): List<PluginOption> = listOf(
    processor.option(KEY_ENABLED, true),
    processor.option(KEY_MASK, "<redacted>"),
    processor.option(KEY_ANNOTATION, "$testPackage.Sensitive"),
  )

  /** @inheritDoc */
  override fun extraSources(): List<SourceFile> = listOf(sensitiveAnnoSrc)

  /** @inheritDoc */
  override fun registrar(): ComponentRegistrar = RedaktRegistrar()

  /** Plugin constants should be expected values. */
  @Test fun `plugin constants should remain valid`() {
    assertEquals("redakt", RedaktPlugin().pluginId)
  }

  @Test fun `should compile without errors for success case`() {
    compile(
      """
      package $testPackage

      data class Test(@Sensitive val a: Int)
      """
    ) {
      // nothing
    }
  }

  @Test fun `should properly redact annotated properties`() {
    compile(
      """
      package elide.tools.kotlin.plugin.redakt.test

      data class Test(@Sensitive val a: Int, val b: String)
      """
    ) {
      assertThat(exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
      val testClass = classLoader.loadClass("$testPackage.Test")
      val instance = testClass.getConstructor(
        Int::class.javaPrimitiveType,
        String::class.java,
      ).newInstance(
        2,
        "hello"
      )
      assertThat(instance.toString()).isEqualTo("Test(a=<redacted>, b=hello)")
    }
  }

  @Test fun `should properly redact annotated classes`() {
    compile(
      """
      package elide.tools.kotlin.plugin.redakt.test

      @Sensitive
      data class Test(val a: Int, val b: String)
      """
    ) {
      assertThat(exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
      val testClass = classLoader.loadClass("$testPackage.Test")
      val instance = testClass.getConstructor(
        Int::class.javaPrimitiveType,
        String::class.java,
      ).newInstance(
        2,
        "hello",
      )
      assertThat(instance.toString()).isEqualTo("Test(<redacted>)")
    }
  }

  @Test fun `verbose mode should trigger extra logging from redakt`() {
    compile(
      """
      package elide.tools.kotlin.plugin.redakt.test

      data class Test(@Sensitive val a: Int, val b: String)
      """,
      verbose = true,
    ) {
      assertThat(exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
      assertThat(messages).contains(":substrate.redakt:")
    }
  }
}
