package ru.kode.detekt.rule

import dev.detekt.test.lint
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class PayloadArgumentNameTest : ShouldSpec({
  should("report error if transitionTo has generic payload name") {
    val code = """
      fun buildMachine() {
        onEach(intent(ViewIntents::navigateBack)) {
          transitionTo { state, payload ->
            state
          }
        }
      }
    """.trimIndent()

    val finding = PayloadArgumentName().lint(code).single()

    finding.message shouldBe "Parameter name \"payload\" is too generic, use more specific name instead"
    finding.shouldStartAt(code, "payload")
  }

  should("report error if transitionTo has generic payload name with type") {
    val code = """
      fun buildMachine() {
        onEach(intent(ViewIntents::navigateBack)) {
          transitionTo { state, payload: Int ->
            state
          }
        }
      }
    """.trimIndent()

    val finding = PayloadArgumentName().lint(code).single()

    finding.message shouldBe "Parameter name \"payload\" is too generic, use more specific name instead"
    finding.shouldStartAt(code, "payload")
  }

  should("report error if action has generic payload name") {
    val code = """
      fun buildMachine() {
        onEach(intent(ViewIntents::navigateBack)) {
          action { _, _, payload ->
            state
          }
        }
      }
    """.trimIndent()

    val finding = PayloadArgumentName().lint(code).single()

    finding.message shouldBe "Parameter name \"payload\" is too generic, use more specific name instead"
    finding.shouldStartAt(code, "payload")
  }

  should("report error if action has generic payload name with type") {
    val code = """
      fun buildMachine() {
        onEach(intent(ViewIntents::navigateBack)) {
          action { _, newState: Int, payload: String ->
            state
          }
        }
      }
    """.trimIndent()

    val finding = PayloadArgumentName().lint(code).single()

    finding.message shouldBe "Parameter name \"payload\" is too generic, use more specific name instead"
    finding.shouldStartAt(code, "payload")
  }

  should("not report error if transitionTo has specific payload name") {
    val code = """
      fun buildMachine() {
        onEach(intent(ViewIntents::navigateBack)) {
          transitionTo { state, navState ->
            state
          }
        }
      }
    """.trimIndent()

    PayloadArgumentName().lint(code).shouldBeEmpty()
  }

  should("not report error if transitionTo has specific payload name with type") {
    val code = """
      fun buildMachine() {
        onEach(intent(ViewIntents::navigateBack)) {
          transitionTo { state, navState: Int ->
            state
          }
        }
      }
    """.trimIndent()

    PayloadArgumentName().lint(code).shouldBeEmpty()
  }

  should("not report error if action has specific payload name") {
    val code = """
      fun buildMachine() {
        onEach(intent(ViewIntents::navigateBack)) {
          action { _, _, navState ->
            state
          }
        }
      }
    """.trimIndent()

    PayloadArgumentName().lint(code).shouldBeEmpty()
  }

  should("not report error if action has specific payload name with type") {
    val code = """
      fun buildMachine() {
        onEach(intent(ViewIntents::navigateBack)) {
          action { _, newState: Int, navState: String ->
            state
          }
        }
      }
    """.trimIndent()

    PayloadArgumentName().lint(code).shouldBeEmpty()
  }

  should("not report errors for action/transitionTo functions outside of onEach") {
    val code = """
      fun someFunc() {
        transitionTo { state, payload: Int ->
          state
        }
        action { _, newState: Int, payload: String ->
          state
        }
      }
    """.trimIndent()

    PayloadArgumentName().lint(code).shouldBeEmpty()
  }

  should("check payload at its position only") {
    val code = """
      fun buildMachine() {
        onEach(intent(ViewIntents::navigateBack)) {
          transitionTo { payload, state -> state }
          action { payload, _, _ -> Unit }
          action { _, payload -> Unit }
          transitionTo(reducer) { state, payload -> state }
        }
      }
    """.trimIndent()

    PayloadArgumentName().lint(code).shouldBeEmpty()
  }

  should("require the onEach lambda to be the second argument") {
    val code = """
      fun buildMachine() {
        onEach { transitionTo { state, payload -> state } }
        onEach(a, b) { transitionTo { state, payload -> state } }
        onEach(intent(ViewIntents::navigateBack), { transitionTo { state, payload -> state } })
      }
    """.trimIndent()

    PayloadArgumentName().lint(code).shouldBeEmpty()
  }

  should("report calls nested anywhere in the onEach lambda") {
    val code = """
      fun buildMachine() {
        onEach(intent(ViewIntents::navigateBack)) {
          if (enabled) {
            run { transitionTo { state, payload -> state } }
          }
        }
      }
    """.trimIndent()

    PayloadArgumentName().lint(code) shouldHaveSize 1
  }

  // 1.x bug: the lambda of an onEach nested in another onEach is checked once per enclosing onEach
  should("report a payload inside nested onEach calls once per onEach") {
    val code = """
      fun buildMachine() {
        onEach(intent(ViewIntents::outer)) {
          onEach(intent(ViewIntents::inner)) {
            transitionTo { state, payload -> state }
          }
        }
      }
    """.trimIndent()

    val findings = PayloadArgumentName().lint(code)

    findings shouldHaveSize 2
    findings.forEach { it.shouldStartAt(code, "payload") }
  }
})
