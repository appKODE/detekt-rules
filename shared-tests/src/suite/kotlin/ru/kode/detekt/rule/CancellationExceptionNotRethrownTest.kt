package ru.kode.detekt.rule

import dev.detekt.test.TestConfig
import dev.detekt.test.lintWithContext
import dev.detekt.test.utils.createEnvironment
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class CancellationExceptionNotRethrownTest : ShouldSpec({
  val environment = createEnvironment()

  should("report a broad catch that does not rethrow") {
    val code = """
      $STUBS
      suspend fun load() {
        try {
          fetch()
        } catch (e: Exception) {
          log(e)
        }
      }
    """.trimIndent()

    val finding = CancellationExceptionNotRethrown().lintWithContext(environment, code).single()

    finding.message shouldBe "Catching \"Exception\" around a suspending call swallows CancellationException: " +
      "rethrow it or call \"ensureActive()\""
    finding.shouldStartAt(code, "catch (e: Exception)")
  }

  should("report every broad type, once per try") {
    val code = """
      $STUBS
      suspend fun load() {
        try { fetch() } catch (e: Throwable) { }
        try { fetch() } catch (e: kotlin.RuntimeException) { throw IllegalStateException(e) }
        try { fetch() } catch (e: IllegalStateException) { } catch (e: Exception) { }
      }
    """.trimIndent()

    CancellationExceptionNotRethrown().lintWithContext(environment, code) shouldHaveSize 3
  }

  should("report a catch of CancellationException that does not rethrow, and not the clauses after it") {
    val code = """
      $STUBS
      suspend fun load() {
        try { fetch() } catch (e: CancellationException) { log(e) } catch (e: Exception) { log(e) }
      }
    """.trimIndent()

    val finding = CancellationExceptionNotRethrown().lintWithContext(environment, code).single()

    finding.message shouldBe "Catching \"CancellationException\" around a suspending call swallows " +
      "CancellationException: rethrow it or call \"ensureActive()\""
    finding.shouldStartAt(code, "catch (e: CancellationException)")
  }

  should("report a try in a suspend lambda of a regular function") {
    val code = """
      $STUBS
      fun start(scope: Scope) {
        scope.launch {
          try { fetch() } catch (e: Exception) { }
        }
      }
    """.trimIndent()

    CancellationExceptionNotRethrown().lintWithContext(environment, code).single()
  }

  should("report a suspending call made in a lambda of an inline function or through a suspend function value") {
    val code = """
      $STUBS
      suspend fun load(items: List<Int>, action: suspend () -> Unit) {
        try { items.forEach { fetch() } } catch (e: Exception) { }
        try { measure { run { fetch() } } } catch (e: Exception) { }
        try { action() } catch (e: Exception) { }
        try { parse("1") + fetch() } catch (e: Exception) { }
      }
    """.trimIndent()

    CancellationExceptionNotRethrown().lintWithContext(environment, code) shouldHaveSize 4
  }

  should("report a suspending call made in a labeled or parenthesized lambda or in an anonymous function") {
    val code = """
      $STUBS
      suspend fun load(items: List<Int>) {
        try { items.forEach loop@{ fetch() } } catch (e: Exception) { }
        try { items.forEach(({ fetch() })) } catch (e: Exception) { }
        try { items.forEach(fun(item: Int) { fetch() }) } catch (e: Exception) { }
      }
    """.trimIndent()

    CancellationExceptionNotRethrown().lintWithContext(environment, code) shouldHaveSize 3
  }

  should("report a try around a nested try, with a finally block or calling a suspending invoke operator") {
    val code = """
      $STUBS
      class Task { suspend operator fun invoke() {} }

      suspend fun load(task: Task) {
        try {
          try { fetch() } catch (e: IOException) { }
        } catch (e: Exception) { }
        try { fetch() } catch (e: Exception) { } finally { log(Exception()) }
        try { task() } catch (e: Exception) { }
      }
    """.trimIndent()

    CancellationExceptionNotRethrown().lintWithContext(environment, code) shouldHaveSize 3
  }

  should("not report a suspending call in a noinline or crossinline lambda of an inline function") {
    val code = """
      $STUBS
      inline fun <T> wrap(crossinline block: suspend () -> T): Any = object { suspend fun run() = block() }
      inline fun <T> keep(noinline block: suspend () -> T): Any = block

      fun build() {
        try { wrap { fetch() } } catch (e: Exception) { }
        try { keep { fetch() } } catch (e: Exception) { }
        try { keep(block = lazy@{ fetch() }) } catch (e: Exception) { }
      }
    """.trimIndent()

    CancellationExceptionNotRethrown().lintWithContext(environment, code).shouldBeEmpty()
  }

  should("not report a try in a sequence or iterator builder") {
    val code = """
      $STUBS
      fun numbers(raw: String) = sequence {
        try { yield(parse(raw)) } catch (e: Exception) { }
        try { yieldAll(listOf(1)) } catch (e: Exception) { }
      }

      fun letters(raw: String) = iterator {
        try { yield(parse(raw)) } catch (e: Exception) { }
      }
    """.trimIndent()

    CancellationExceptionNotRethrown().lintWithContext(environment, code).shouldBeEmpty()
  }

  should("not report a try block that makes no suspending call itself") {
    val code = """
      $STUBS
      suspend fun load(scope: Scope, raw: String) {
        try { parse(raw) } catch (e: Exception) { }
        try { scope.launch { fetch() } } catch (e: Exception) { }
        try { later { parse(raw) } } catch (e: Exception) { }
        try {
          suspend fun local() = fetch()
          val callback = object : Runnable {
            override fun run() { scope.launch { fetch() } }
          }
        } catch (e: Exception) { }
        try { unknown() } catch (e: Exception) { }
        try { parse(raw) } catch (e: Exception) { fetch() } finally { fetch() }
      }

      fun regular(raw: String) {
        try { parse(raw) } catch (e: Exception) { }
      }
    """.trimIndent()

    CancellationExceptionNotRethrown().lintWithContext(environment, code).shouldBeEmpty()
  }

  should("not report a catch that rethrows or checks for cancellation") {
    val code = """
      $STUBS
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

    CancellationExceptionNotRethrown().lintWithContext(environment, code).shouldBeEmpty()
  }

  should("not report narrow catches and try without catch") {
    val code = """
      $STUBS
      suspend fun load() {
        try { fetch() } catch (e: IOException) { log(e) }
        try { fetch() } catch (e: IOException) { log(e) } catch (e: CancellationException) { throw e }
        try { fetch() } finally { log(Exception()) }
      }
    """.trimIndent()

    CancellationExceptionNotRethrown().lintWithContext(environment, code).shouldBeEmpty()
  }

  should("treat a call of a configured function as a rethrow") {
    val code = """
      $STUBS
      suspend fun load() {
        try {
          fetch()
        } catch (e: Exception) {
          e.rethrowCancellation()
          log(e)
        }
      }
    """.trimIndent()

    CancellationExceptionNotRethrown().lintWithContext(environment, code) shouldHaveSize 1
    val config = TestConfig("rethrowFunctions" to listOf("rethrowCancellation"))
    CancellationExceptionNotRethrown(config).lintWithContext(environment, code).shouldBeEmpty()
  }
})

// kotlinx.coroutines is not on the test classpath
private const val STUBS = """package work

      import java.io.IOException
      import kotlin.coroutines.CoroutineContext
      import kotlin.coroutines.cancellation.CancellationException
      import kotlin.coroutines.coroutineContext

      class Scope { fun launch(block: suspend () -> Unit) {} }
      suspend fun fetch(): Int = 1
      fun parse(raw: String): Int = raw.length
      fun log(error: Throwable) {}
      fun later(block: () -> Unit) {}
      inline fun <T> measure(block: () -> T): T = block()
      fun CoroutineContext.ensureActive() {}
      fun Throwable.rethrowCancellation() {}
"""
