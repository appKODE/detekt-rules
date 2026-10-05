package ru.kode.detekt.rule

import dev.detekt.test.lintWithContext
import dev.detekt.test.utils.createEnvironment
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class UnhandledViewIntentTest : ShouldSpec({
  val environment = createEnvironment()

  should("report the intents that the machine does not refer to") {
    val code = """
      $STUBS
      class MainViewModel {
        fun buildMachine() = machine {
          onEach(intent(Intents::onRefresh))
        }
      }
    """.trimIndent()

    val finding = UnhandledViewIntent().lintWithContext(environment, code).single()

    finding.message shouldBe
      "View intents are never handled in the machine of \"MainViewModel\": Intents.onItemClick, Intents.onBack"
    finding.shouldStartAt(code, "machine {")
    finding.entity.location.text.run { end - start } shouldBe "machine".length
  }

  should("count references made outside the machine call, and report nested view models on their own") {
    val code = """
      $STUBS
      class MainViewModel {
        fun buildMachine() = machine {
          onEach(intent(Intents::onRefresh))
          configureNavigation()
        }

        private fun configureNavigation() {
          onEach(intent(Intents::onBack))
        }

        class Nested {
          fun buildMachine() = machine {
            onEach(intent(screen.Intents::onItemClick))
          }
        }
      }
    """.trimIndent()

    val findings = UnhandledViewIntent().lintWithContext(environment, code)

    findings.map { it.message } shouldContainExactlyInAnyOrder listOf(
      "View intents are never handled in the machine of \"Nested\": Intents.onRefresh, Intents.onBack",
      "View intents are never handled in the machine of \"MainViewModel\": Intents.onItemClick",
    )
  }

  should("name the intents class of each unhandled intent when the machine refers to several") {
    val code = """
      $STUBS
      class DialogIntents : ViewIntents() {
        val onConfirm = intent("confirm")
        val onBack = intent("back")
      }

      class MainViewModel {
        fun buildMachine() = machine {
          onEach(intent(Intents::onRefresh))
          onEach(intent(Intents::onItemClick))
          onEach(intent(DialogIntents::onConfirm))
        }
      }
    """.trimIndent()

    val finding = UnhandledViewIntent().lintWithContext(environment, code).single()

    finding.message shouldBe
      "View intents are never handled in the machine of \"MainViewModel\": Intents.onBack, DialogIntents.onBack"
  }

  should("not report when every intent is handled") {
    val code = """
      $STUBS
      class MainViewModel {
        fun buildMachine() = machine {
          onEach(intent(Intents::onRefresh))
          onEach(intent(Intents::onItemClick))
          onEach(intent(Intents::onBack))
          onEach(Intents::title)
          onEach(Other::value)
          onEach(Missing::value)
          onEach(::machine)
        }
      }
    """.trimIndent()

    UnhandledViewIntent().lintWithContext(environment, code).shouldBeEmpty()
  }

  should("not report a class without a machine or without references to intents") {
    val code = """
      $STUBS
      class Binder {
        fun bind() {
          onEach(intent(Intents::onRefresh))
        }
      }

      class EmptyViewModel {
        fun buildMachine() = machine { }
      }
    """.trimIndent()

    UnhandledViewIntent().lintWithContext(environment, code) shouldHaveSize 0
  }
})

private const val STUBS = """package screen

      open class ViewIntents {
        fun intent(name: String): Int = name.length
      }

      class Intents : ViewIntents() {
        val onRefresh = intent(name = "refresh")
        val onItemClick = intent(name = "itemClick")
        val onBack = intent("back")
        val title = "not an intent"
      }

      class Other {
        val value = 1
      }

      fun machine(block: () -> Unit) = block
      fun intent(reference: Any) = reference
      fun onEach(source: Any) = source
"""
