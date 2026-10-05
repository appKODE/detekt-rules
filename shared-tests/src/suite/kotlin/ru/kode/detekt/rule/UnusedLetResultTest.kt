package ru.kode.detekt.rule

import dev.detekt.test.lint
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class UnusedLetResultTest : ShouldSpec({
  should("report a let call used as a statement") {
    val code = """
      fun show(error: String?) {
        error?.let { println(it) }
        println()
      }
    """.trimIndent()

    val finding = UnusedLetResult().lint(code).single()

    finding.message shouldBe "Result of \"let\" is not used: use an \"if\" check or \"also\" instead"
    finding.shouldStartAt(code, "let {")
  }

  should("report the last statement of block bodies whose value is discarded") {
    val code = """
      class Presenter(private val error: String?) {
        val text: String
          get() {
            error?.let { println(it) }
          }

        init {
          error.let { println(it) }
        }

        constructor() : this(null) {
          error?.let { println(it) }
        }

        fun show() {
          error?.let { println(it) }
        }
      }
    """.trimIndent()

    UnusedLetResult().lint(code) shouldHaveSize 4
  }

  should("report let calls in branches of statements") {
    val code = """
      fun show(error: String?, mode: Int) {
        if (mode == 0) error?.let { println(it) } else error?.let { print(it) }
        when (mode) {
          1 -> error?.let { println(it) }
          else -> {
            (error?.let { println(it) })
          }
        }
        for (index in 0..mode) error?.let { println(it) }
        while (mode > 0) {
          error?.let { println(it) }
        }
        try {
          error?.let { println(it) }
        } catch (e: Exception) {
          error?.let { println(it) }
        } finally {
          error?.let { println(it) }
        }
        loop@ error?.let { println(it) }
        println()
      }
    """.trimIndent()

    UnusedLetResult().lint(code) shouldHaveSize 10
  }

  should("not report a let call whose result is used") {
    val code = """
      fun length(text: String?): Int = text?.let { it.length } ?: 0

      fun describe(text: String?, mode: Int): String? {
        val trimmed = text?.let { it.trim() }
        val chosen = if (mode == 0) text?.let { it.trim() } else null
        val matched = when (mode) {
          1 -> text?.let { it.trim() }
          else -> null
        }
        val attempted = try {
          text?.let { it.trim() }
        } catch (e: Exception) {
          text?.let { it.trim() }
        }
        text?.let { it.trim() }?.length
        text?.let { it.trim() } ?: return null
        consume(text?.let { it.trim() })
        return text?.let { it.trim() }
      }
    """.trimIndent()

    UnusedLetResult().lint(code).shouldBeEmpty()
  }

  should("report a let call that is not the last statement of a lambda") {
    val code = """
      fun show(errors: List<String?>) {
        errors.forEach {
          it?.let { error -> println(error) }
          println("done")
        }
      }
    """.trimIndent()

    UnusedLetResult().lint(code).single().shouldStartAt(code, "let {")
  }

  should("not report the last statement of a lambda") {
    val code = """
      fun show(errors: List<String?>) {
        errors.forEach { error ->
          error?.let { println(it) }
        }
        errors.map { error ->
          if (error != null) error.let { it.length } else 0
        }
      }
    """.trimIndent()

    UnusedLetResult().lint(code).shouldBeEmpty()
  }

  should("not report let without a lambda, a let function of its own and other scope functions") {
    val code = """
      fun show(error: String?) {
        error?.let(::println)
        let { println(it) }
        error?.also { println(it) }
        error?.run { println(this) }
        println()
      }
    """.trimIndent()

    UnusedLetResult().lint(code).shouldBeEmpty()
  }
})
