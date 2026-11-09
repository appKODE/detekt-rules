package ru.kode.detekt.rule

import dev.detekt.test.lintWithContext
import dev.detekt.test.utils.createEnvironment
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class InconsistentWhenTypeCheckTest : ShouldSpec({
  val environment = createEnvironment()

  should("report an object compared by equality next to an is-check") {
    val code = """
      $STUBS
      fun render(state: State) = when (state) {
        is State.Content -> 1
        State.Loading -> 2
        is State.Failed -> 3
      }
    """.trimIndent()

    val finding = InconsistentWhenTypeCheck().lintWithContext(environment, code).single()

    finding.message shouldBe "Use \"is State.Loading\" like the other branches of this \"when\" do"
    finding.shouldStartAt(code, "State.Loading ->")
  }

  should("report every equality check of an object, also in a multi-condition branch and for a library object") {
    val code = """
      $STUBS
      fun render(state: Any) {
        when (val current = state) {
          Standalone, is State.Content -> Unit
          ui.state.State.Failed -> Unit
          Unit -> Unit
          else -> Unit
        }
      }
    """.trimIndent()

    InconsistentWhenTypeCheck().lintWithContext(environment, code).map { it.message }.shouldContainExactly(
      "Use \"is Standalone\" like the other branches of this \"when\" do",
      "Use \"is ui.state.State.Failed\" like the other branches of this \"when\" do",
      "Use \"is Unit\" like the other branches of this \"when\" do",
    )
  }

  should("report an object in parentheses and a type alias of an object") {
    val code = """
      $STUBS
      typealias Progress = State.Loading
      typealias Plain = WithCompanion
      typealias Shared = WithCompanion.Companion

      fun render(state: Any) = when (state) {
        is State.Content -> 1
        Progress -> 2
        (State.Failed) -> 3
        ((Standalone)) -> 4
        Plain, Shared, (MAX_VALUE) -> 5
        else -> 0
      }
    """.trimIndent()

    InconsistentWhenTypeCheck().lintWithContext(environment, code).map { it.message }.shouldContainExactly(
      "Use \"is Progress\" like the other branches of this \"when\" do",
      "Use \"is State.Failed\" like the other branches of this \"when\" do",
      "Use \"is Standalone\" like the other branches of this \"when\" do",
    )
  }

  should("not report a when using is-checks only, or equality checks only") {
    val code = """
      $STUBS
      fun render(state: State) {
        when (state) {
          is State.Content -> Unit
          is State.Loading -> Unit
          is State.Failed -> Unit
        }
        when (state) {
          State.Failed -> Unit
          State.Loading -> Unit
          else -> Unit
        }
      }
    """.trimIndent()

    InconsistentWhenTypeCheck().lintWithContext(environment, code).shouldBeEmpty()
  }

  should("not report constants, enum entries, literals, null, calls and ranges next to an is-check") {
    val code = """
      $STUBS
      fun render(value: Any?) {
        when (value) {
          is String -> Unit
          null -> Unit
          1, "text" -> Unit
          MAX_VALUE, Dp.Unspecified, Color.Red, WithCompanion -> Unit
          create() -> Unit
          State.create().Loading -> Unit
          in 1..2 -> Unit
          Missing, Missing.Loading -> Unit
          else -> Unit
        }
      }
    """.trimIndent()

    InconsistentWhenTypeCheck().lintWithContext(environment, code).shouldBeEmpty()
  }

  should("not report a when without a subject") {
    val code = """
      $STUBS
      fun render(state: Any) {
        when {
          state is String -> Unit
          state == Standalone -> Unit
        }
      }
    """.trimIndent()

    InconsistentWhenTypeCheck().lintWithContext(environment, code).shouldBeEmpty()
  }
})

private const val STUBS = """package ui.state

      sealed interface State {
        class Content(val items: Int) : State
        object Loading : State
        data object Failed : State

        companion object {
          fun create(): Holder = Holder()
        }
      }

      class Holder { val Loading = 1 }
      object Standalone
      enum class Color { Red }
      class Dp { companion object { val Unspecified = Dp() } }
      class WithCompanion { companion object }
      const val MAX_VALUE = 1
      fun create(): Any = 1
"""
