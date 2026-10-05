package ru.kode.detekt.rule

import dev.detekt.test.lint
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class CoroutineScopeNotCancelledTest : ShouldSpec({
  should("report a scope that is never cancelled") {
    val code = """
      class Player {
        private val scope = CoroutineScope(Dispatchers.Main)

        fun play() {
          scope.launch { }
        }
      }
    """.trimIndent()

    val finding = CoroutineScopeNotCancelled().lint(code).single()

    finding.message shouldBe "CoroutineScope \"scope\" is created in class \"Player\" but never cancelled"
    finding.shouldStartAt(code, "private val scope")
  }

  should("report only the scopes left without a cancel call") {
    val code = """
      class Player {
        private val ui = MainScope()
        private val io = CoroutineScope(Dispatchers.IO)
        private val other = CoroutineScope(Dispatchers.Default)

        fun release(player: Player) {
          io.cancel()
          player.other.cancel()
          cancel()
          create().cancel()
          other.coroutineContext.cancelChildren()
        }
      }
    """.trimIndent()

    CoroutineScopeNotCancelled().lint(code) shouldHaveSize 2
  }

  should("not report a scope cancelled through any receiver chain starting with it") {
    val code = """
      class Player {
        private val first = CoroutineScope(Dispatchers.Main)
        private val second = CoroutineScope(Dispatchers.Main)
        private val third = CoroutineScope(Dispatchers.Main)
        private var fourth: CoroutineScope? = MainScope()
        private val fifth = CoroutineScope(Dispatchers.Main)
        private val sixth = CoroutineScope(Dispatchers.Main)

        fun release() {
          first.cancel()
          this.second.cancel("released")
          third.coroutineContext.cancel()
          fourth?.cancel()
          fifth.coroutineContext[Job]?.cancel()
          (sixth).cancel()
        }
      }
    """.trimIndent()

    CoroutineScopeNotCancelled().lint(code).shouldBeEmpty()
  }

  should("report a scope when only a job launched in it is cancelled") {
    val code = """
      class Player {
        private val scope = CoroutineScope(Dispatchers.Main)

        fun stop() {
          scope.launch { }.cancel()
          scope.coroutineContext.job.children.first().cancel()
        }
      }
    """.trimIndent()

    CoroutineScopeNotCancelled().lint(code) shouldHaveSize 1
  }

  should("not count a cancel call of a nested class, and count one made in an anonymous object") {
    val code = """
      class Outer {
        private val scope = CoroutineScope(Dispatchers.Main)
        private val other = CoroutineScope(Dispatchers.Main)
        private val listener = object : Listener {
          override fun onClose() {
            other.cancel()
          }
        }

        class Nested {
          private val scope = CoroutineScope(Dispatchers.Main)

          fun release() {
            scope.cancel()
          }
        }
      }
    """.trimIndent()

    val finding = CoroutineScopeNotCancelled().lint(code).single()

    finding.message shouldBe "CoroutineScope \"scope\" is created in class \"Outer\" but never cancelled"
  }

  should("not report a scope built from a job that is cancelled") {
    val code = """
      class Player {
        private val job = SupervisorJob()
        private val scope = CoroutineScope(Dispatchers.Main + job)

        fun release() {
          job.cancel()
        }
      }
    """.trimIndent()

    CoroutineScopeNotCancelled().lint(code).shouldBeEmpty()
  }

  should("not report objects, injected scopes and scopes that are not properties of a class") {
    val code = """
      object Holder {
        val scope = CoroutineScope(Dispatchers.Main)
      }

      val global = CoroutineScope(Dispatchers.Main)

      class Player(private val injected: CoroutineScope = CoroutineScope(Dispatchers.Main)) {
        private val derived = injected + Dispatchers.IO
        private val name = "player"

        fun play() {
          val local = CoroutineScope(Dispatchers.Main)
        }
      }

      interface Marker
    """.trimIndent()

    CoroutineScopeNotCancelled().lint(code).shouldBeEmpty()
  }
})
