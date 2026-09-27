package ru.kode.detekt.rule

import dev.detekt.test.lint
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class UseOnStartEmitTest : ShouldSpec({
  should("report error for flow builders") {
    val code = """
      fun main() {
        val flow = flow { 1 }.onStart {
          emit(3)
        }
      }
    """.trimIndent()

    val finding = UseOnStartEmit().lint(code).single()

    finding.message shouldBe "Use Flow.onStartEmit() instead of Flow.onStart()"
    finding.shouldStartAt(code, "onStart {")
  }

  should("report error for flowOf") {
    val code = """
      fun main() {
        val flow = flowOf(3).onStart {
          emit(3)
        }
      }
    """.trimIndent()

    UseOnStartEmit().lint(code) shouldHaveSize 1
  }

  should("report error for method returning flow") {
    val code = """
      class MyRepository {
        fun users(): Flow<Int>
      }

      fun main() {
        val repository = MyRepository()
        val flow = repository.users().onStart { emit(3) }.map { it.toString() }
      }
    """.trimIndent()

    val finding = UseOnStartEmit().lint(code).single()

    finding.shouldStartAt(code, "onStart { emit(3) }")
  }

  should("not report onStart doing more than a top-level emit") {
    val code = """
      fun main() {
        flowOf(3).onStart { log("start") }
        flowOf(3).onStart { if (ready) emit(3) }
        flowOf(3).onStart(::emitDefault)
        flowOf(3).onEach { emit(3) }
      }
    """.trimIndent()

    UseOnStartEmit().lint(code).shouldBeEmpty()
  }

  should("report emit among other statements") {
    val code = """
      fun main() {
        flowOf(3).onStart {
          log("start")
          emit(3)
        }
      }
    """.trimIndent()

    UseOnStartEmit().lint(code) shouldHaveSize 1
  }

  should("not report onStart without a receiver") {
    val code = """
      fun main() {
        onStart { emit(3) }
      }
    """.trimIndent()

    UseOnStartEmit().lint(code).shouldBeEmpty()
  }

  // 1.x reported only the outermost one: it did not descend into a reported chain
  should("report every offending onStart of a chain and its lambdas") {
    val code = """
      fun main() {
        flowOf(1).onStart { emit(0) }.map { flowOf(2).onStart { emit(1) } }.onStart { emit(-1) }
      }
    """.trimIndent()

    val findings = UseOnStartEmit().lint(code).sortedBy { it.entity.location.source.column }

    findings shouldHaveSize 3
    findings[0].shouldStartAt(code, "onStart { emit(0) }")
    findings[1].shouldStartAt(code, "onStart { emit(1) }")
    findings[2].shouldStartAt(code, "onStart { emit(-1) }")
  }
})
