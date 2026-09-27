package ru.kode.detekt.rule

import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class ImmutableDataClassTest : ShouldSpec({
  should("report error for data class in a ui package") {
    val code = """
      package ru.kode.example.feature.ui.screen
      data class SomeState(val i: Int = 3)
    """.trimIndent()

    val finding = ImmutableDataClass().lint(code).single()

    finding.message shouldBe "Data class \"SomeState\" is missing @Immutable annotation"
    finding.shouldStartAt(code, "data class")
  }

  should("not report error for data class in a ui package with @Immutable annotation") {
    val code = """
      package ru.kode.example.feature.ui.screen

      import androidx.compose.runtime.Immutable

      @Immutable
      data class SomeState(val i: Int = 3)
    """.trimIndent()

    ImmutableDataClass().lint(code).shouldBeEmpty()
  }

  should("not report error for data class with fully qualified @Immutable annotation") {
    val code = """
      package ru.kode.example.feature.ui.screen

      @androidx.compose.runtime.Immutable
      data class SomeState(val i: Int = 3)
    """.trimIndent()

    ImmutableDataClass().lint(code).shouldBeEmpty()
  }

  should("report finding at the annotations of an annotated data class") {
    val code = """
      package ru.kode.example.feature.ui.screen

      @Stable
      data class SomeState(val i: Int = 3)
    """.trimIndent()

    ImmutableDataClass().lint(code).single().shouldStartAt(code, "@Stable")
  }

  should("not report error for data class in a non-ui package") {
    val code = """
      package ru.kode.example.feature.domain
      data class SomeState(val i: Int = 3)
    """.trimIndent()

    ImmutableDataClass().lint(code).shouldBeEmpty()
  }

  should("match the ui package segment exactly, at any depth") {
    val uiRoot = "package ui\ndata class A(val i: Int)"
    val uiKit = "package ru.kode.uikit.screen\ndata class A(val i: Int)"
    val noPackage = "data class A(val i: Int)"

    ImmutableDataClass().lint(uiRoot) shouldHaveSize 1
    ImmutableDataClass().lint(uiKit).shouldBeEmpty()
    ImmutableDataClass().lint(noPackage).shouldBeEmpty()
  }

  should("not report error for non data class in a ui package") {
    val code = """
      package ru.kode.example.feature.ui.screen
      class SomeState(val i: Int = 3)
    """.trimIndent()

    ImmutableDataClass().lint(code).shouldBeEmpty()
  }

  should("report nested data classes") {
    val code = """
      package ru.kode.example.feature.ui.screen
      class Screen {
        data class State(val i: Int)
      }
    """.trimIndent()

    ImmutableDataClass().lint(code).single().message shouldBe "Data class \"State\" is missing @Immutable annotation"
  }

  should("not report error for descendants of ignored class") {
    val code = """
      package ru.kode.example.feature.ui.screen
      sealed class SomeFlowEvent {
        data class SetHello(val foo: Int) : SomeFlowEvent()
      }
      sealed interface OtherFlowEvent {
        data class SetHello(val foo: Int) : OtherFlowEvent
      }
      data class SetWorld(val foo: Int) : OtherFlowEvent
    """.trimIndent()

    val findings = ImmutableDataClass(
      ignoreDescendantsOf = listOf("SomeFlowEvent", "OtherFlowEvent"),
    ).lint(code)

    findings.shouldBeEmpty()
  }

  should("read ignoreDescendantsOf from config") {
    val code = """
      package ru.kode.example.feature.ui.screen
      data class SetWorld(val foo: Int) : OtherFlowEvent
    """.trimIndent()

    ImmutableDataClass(TestConfig("ignoreDescendantsOf" to listOf("OtherFlowEvent"))).lint(code).shouldBeEmpty()
  }

  // PSI only: only supertypes written in the declaration count, by their short name
  should("ignore only direct descendants, by the short supertype name") {
    val code = """
      package ru.kode.example.feature.ui.screen
      sealed interface Event : FlowEvent
      data class Direct(val foo: Int) : ru.kode.FlowEvent
      data class Generic(val foo: Int) : FlowEvent<Int>
      data class Indirect(val foo: Int) : Event
    """.trimIndent()

    val finding = ImmutableDataClass(ignoreDescendantsOf = listOf("FlowEvent")).lint(code).single()

    finding.message shouldBe "Data class \"Indirect\" is missing @Immutable annotation"
  }
})
