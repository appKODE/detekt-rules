package ru.kode.detekt.rule

import io.gitlab.arturbosch.detekt.test.lint
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldBeEmpty

/**
 * detekt1 lets `lint` run type-resolution rules with an empty BindingContext (detekt-core itself skips them when
 * there is no classpath). The type-resolved rules must then report nothing, as 1.x did.
 */
class NoTypeResolutionTest : ShouldSpec({
  should("skip the check without type resolution: MissingTypeDeclaration") {
    val code = """
      class MyClass() {
        val number = 123
      }
    """.trimIndent()

    MissingTypeDeclaration().lint(code).shouldBeEmpty()
  }

  should("skip the check without type resolution: BlockingSqlDelightCall") {
    val code = """
      package app.cash.sqldelight

      interface Transacter { fun transaction() }

      suspend fun run(db: Transacter) {
        db.transaction()
      }
    """.trimIndent()

    BlockingSqlDelightCall().lint(code).shouldBeEmpty()
  }

  should("skip the check without type resolution: PublicSuspendFunctionInReactiveModel") {
    val code = """
      open class ReactiveModel

      class ProfileModel : ReactiveModel() {
        suspend fun load(): Int = 1
      }
    """.trimIndent()

    PublicSuspendFunctionInReactiveModel().lint(code).shouldBeEmpty()
  }

  should("skip the check without type resolution: UnhandledViewIntent") {
    val code = """
      class Intents {
        val onRefresh = intent("refresh")
        val onBack = intent("back")
      }

      class MainViewModel {
        fun buildMachine() = machine { intent(Intents::onRefresh) }
      }
    """.trimIndent()

    UnhandledViewIntent().lint(code).shouldBeEmpty()
  }

  should("skip the check without type resolution: InconsistentWhenTypeCheck") {
    val code = """
      object Loading

      fun render(state: Any) = when (state) {
        is String -> 1
        Loading -> 2
        else -> 3
      }
    """.trimIndent()

    InconsistentWhenTypeCheck().lint(code).shouldBeEmpty()
  }

  should("skip the check without type resolution: UnusedLetResult") {
    val code = """
      fun show(error: String?) {
        error?.let { println(it) }
      }
    """.trimIndent()

    UnusedLetResult().lint(code).shouldBeEmpty()
  }

  should("skip the check without type resolution: SealedClassCouldBeEnum") {
    val code = """
      sealed interface Direction {
        object Up : Direction
        object Down : Direction
      }
    """.trimIndent()

    SealedClassCouldBeEnum().lint(code).shouldBeEmpty()
  }

  should("skip the check without type resolution: CancellationExceptionNotRethrown") {
    val code = """
      suspend fun fetch() {}

      suspend fun load() {
        try { fetch() } catch (e: Exception) { }
      }
    """.trimIndent()

    CancellationExceptionNotRethrown().lint(code).shouldBeEmpty()
  }
})
