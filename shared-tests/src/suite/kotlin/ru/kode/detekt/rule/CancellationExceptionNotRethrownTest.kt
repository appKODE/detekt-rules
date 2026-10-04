package ru.kode.detekt.rule

import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class CancellationExceptionNotRethrownTest : ShouldSpec({
  should("report a broad catch that does not rethrow") {
    val code = """
      suspend fun load() {
        try {
          fetch()
        } catch (e: Exception) {
          log(e)
        }
      }
    """.trimIndent()

    val finding = CancellationExceptionNotRethrown().lint(code).single()

    finding.message shouldBe "Catching \"Exception\" in a suspend function swallows CancellationException: " +
      "rethrow it or call \"ensureActive()\""
    finding.shouldStartAt(code, "catch (e: Exception)")
  }

  should("report every broad type, once per try, also in lambdas and local functions of a suspend function") {
    val code = """
      suspend fun load(items: List<Int>) {
        try { fetch() } catch (e: Throwable) { }
        try { fetch() } catch (e: kotlin.RuntimeException) { throw IllegalStateException(e) }
        try { fetch() } catch (e: IllegalStateException) { } catch (e: Exception) { }
        try { fetch() } catch (e: CancellationException) { log(e) } catch (e: Exception) { }
        items.forEach {
          try { fetch() } catch (e: Exception) { }
        }
        fun local() {
          try { fetch() } catch (e: Exception) { }
        }
      }
    """.trimIndent()

    CancellationExceptionNotRethrown().lint(code) shouldHaveSize 6
  }

  should("not report a catch that rethrows or checks for cancellation") {
    val code = """
      suspend fun load() {
        try { fetch() } catch (e: Exception) { throw e }
        try { fetch() } catch (e: Exception) {
          if (e is CancellationException) throw e
          log(e)
        }
        try { fetch() } catch (e: Throwable) {
          coroutineContext.ensureActive()
          log(e)
        }
        try { fetch() } catch (e: CancellationException) { throw e } catch (e: Exception) { log(e) }
      }
    """.trimIndent()

    CancellationExceptionNotRethrown().lint(code).shouldBeEmpty()
  }

  should("not report narrow catches and try without catch") {
    val code = """
      suspend fun load() {
        try { fetch() } catch (e: IOException) { log(e) }
        try { fetch() } catch (e: CancellationException) { log(e) }
        try { fetch() } finally { close() }
      }
    """.trimIndent()

    CancellationExceptionNotRethrown().lint(code).shouldBeEmpty()
  }

  should("not report regular functions, and report a nested suspend function once") {
    val code = """
      fun regular() {
        try { fetch() } catch (e: Exception) { }
        suspend fun nested() {
          try { fetch() } catch (e: Exception) { }
        }
      }

      suspend fun outer() {
        suspend fun nested() {
          try { fetch() } catch (e: Exception) { }
        }
      }
    """.trimIndent()

    CancellationExceptionNotRethrown().lint(code) shouldHaveSize 2
  }

  should("treat a call of a configured function as a rethrow") {
    val code = """
      suspend fun load() {
        try {
          fetch()
        } catch (e: Exception) {
          e.rethrowCancellation()
          log(e)
        }
      }
    """.trimIndent()

    CancellationExceptionNotRethrown().lint(code) shouldHaveSize 1
    val config = TestConfig("rethrowFunctions" to listOf("rethrowCancellation"))
    CancellationExceptionNotRethrown(config).lint(code).shouldBeEmpty()
  }
})
