package ru.kode.detekt.rule

import dev.detekt.test.lint
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

class NullableValueClassPropertyTest : ShouldSpec({
  should("report a value class wrapping a nullable type") {
    val code = """
      @JvmInline
      value class UserId(val value: String?)
    """.trimIndent()

    val finding = NullableValueClassProperty().lint(code).single()

    finding.message shouldBe
      "Value class \"UserId\" wraps a nullable type: make the property non-null and use \"UserId?\""
    finding.shouldStartAt(code, "val value")
  }

  should("report a nested value class wrapping a nullable generic type") {
    val code = """
      class Outer {
        @JvmInline
        value class Ids(val value: List<String>?)
      }
    """.trimIndent()

    NullableValueClassProperty().lint(code).single().shouldStartAt(code, "val value")
  }

  should("not report a value class wrapping a non-null type") {
    val code = """
      @JvmInline
      value class UserId(val value: String)

      @JvmInline
      value class Names(val value: List<String?>)
    """.trimIndent()

    NullableValueClassProperty().lint(code).shouldBeEmpty()
  }

  should("not report nullable properties of other classes") {
    val code = """
      data class User(val name: String?)
      class Holder(val value: String?)
      value class Broken
    """.trimIndent()

    NullableValueClassProperty().lint(code).shouldBeEmpty()
  }
})
