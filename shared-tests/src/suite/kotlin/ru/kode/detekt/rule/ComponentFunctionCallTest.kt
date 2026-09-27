package ru.kode.detekt.rule

import dev.detekt.test.lint
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class ComponentFunctionCallTest : ShouldSpec({
  should("report error for component function call") {
    val code = """
      fun main() {
        val person = Pair(nameValue, ageValue)
        val name = person.component1()
      }
    """.trimIndent()

    val finding = ComponentFunctionCall().lint(code).single()

    finding.message shouldBe
      "Instead of calling \"componentN\" functions, use property access or any other means available"
    finding.shouldStartAt(code, "component1()")
  }

  should("report error for each component function call") {
    val code = """
      data class Person(val name: String, val age: Int, val sex: String, val address: String)
      fun main() {
        val person = Person(nameValue, ageValue)
        person.component1()
        person.component2()
        person.component3()
        person.component4()
      }
    """.trimIndent()

    ComponentFunctionCall().lint(code) shouldHaveSize 4
  }

  should("ignore component0 function call") {
    val code = """
      class Test {
        fun component0() = 1
      }
      fun main() {
        val test = Test()
        test.component0()
      }
    """.trimIndent()

    ComponentFunctionCall().lint(code).shouldBeEmpty()
  }

  should("return success when component function called with parameters") {
    val code = """
      class Test {
        fun component1(value1: Int, value2: int, value3: Int) = value1 + value2 + value3
        fun component2(value: Int) = value
      }
      fun main() {
        val test = Test()
        test.component1(1, 2, value3 = 12)
        test.component2(1)
      }
    """.trimIndent()

    ComponentFunctionCall().lint(code).shouldBeEmpty()
  }

  should("return success when no component function called") {
    val code = """
      data class Person(val name: String, val age: Int)
      fun main() {
        val person = Person()
        val name = person.name
        val age = person.age
      }
    """.trimIndent()

    ComponentFunctionCall().lint(code).shouldBeEmpty()
  }

  should("match multi-digit component numbers only without a leading zero") {
    val code = """
      fun main() {
        tuple.component10()
        tuple.component01()
        tuple.componentN()
      }
    """.trimIndent()

    ComponentFunctionCall().lint(code).single().shouldStartAt(code, "component10()")
  }

  should("not report component calls without a receiver") {
    val code = """
      data class Person(val name: String) {
        fun first() = component1()
      }
    """.trimIndent()

    ComponentFunctionCall().lint(code).shouldBeEmpty()
  }

  // 1.x quirk: the rule stops at the first match of a chain (outermost first) and does not descend into it
  should("report only the outermost component call of a chain") {
    val code = """
      fun main() {
        pair.component1().let { it.component2() }.component2()
      }
    """.trimIndent()

    ComponentFunctionCall().lint(code).single().shouldStartAt(code, "component2()\n")
  }
})
