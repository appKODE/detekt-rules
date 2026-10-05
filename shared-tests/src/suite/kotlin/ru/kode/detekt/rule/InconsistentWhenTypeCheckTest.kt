package ru.kode.detekt.rule

import dev.detekt.test.lint
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class InconsistentWhenTypeCheckTest : ShouldSpec({
  should("report an object compared by equality next to an is-check") {
    val code = """
      fun render(state: State) = when (state) {
        is State.Content -> 1
        State.Loading -> 2
      }
    """.trimIndent()

    val finding = InconsistentWhenTypeCheck().lint(code).single()

    finding.message shouldBe "Use \"is State.Loading\" like the other branches of this \"when\" do"
    finding.shouldStartAt(code, "State.Loading")
  }

  should("report every equality check of an object, also in a multi-condition branch") {
    val code = """
      fun render(state: Any) {
        when (val current = state) {
          Loading, is Content -> Unit
          ui.state.Failed -> Unit
          else -> Unit
        }
      }
    """.trimIndent()

    InconsistentWhenTypeCheck().lint(code) shouldHaveSize 2
  }

  should("not report a when using is-checks only, or equality checks only") {
    val code = """
      fun render(state: State) {
        when (state) {
          is State.Content -> Unit
          is State.Loading -> Unit
        }
        when (state) {
          State.Content -> Unit
          State.Loading -> Unit
        }
      }
    """.trimIndent()

    InconsistentWhenTypeCheck().lint(code).shouldBeEmpty()
  }

  should("not report constants, literals, null, calls and ranges next to an is-check") {
    val code = """
      fun render(value: Any?) {
        when (value) {
          is String -> Unit
          null -> Unit
          1, "text" -> Unit
          MAX_VALUE, Limits.MIN_VALUE, X -> Unit
          default -> Unit
          create() -> Unit
          State.create().Loading -> Unit
          in 1..2 -> Unit
          else -> Unit
        }
      }
    """.trimIndent()

    InconsistentWhenTypeCheck().lint(code).shouldBeEmpty()
  }

  // a known limit: an object is recognised by its PascalCase name
  should("report a PascalCase constant or enum entry next to an is-check") {
    val code = """
      fun describe(value: Any) = when (value) {
        is String -> "text"
        Dp.Unspecified -> "no size"
        Color.Red -> "red"
        else -> "other"
      }
    """.trimIndent()

    InconsistentWhenTypeCheck().lint(code) shouldHaveSize 2
  }

  should("not report a when without a subject") {
    val code = """
      fun render(state: Any) {
        when {
          state is String -> Unit
          state == Loading -> Unit
        }
      }
    """.trimIndent()

    InconsistentWhenTypeCheck().lint(code).shouldBeEmpty()
  }
})
