package ru.kode.detekt.rule

import dev.detekt.test.TestConfig
import dev.detekt.test.lintWithContext
import dev.detekt.test.utils.createEnvironment
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class MissingTypeDeclarationTest : ShouldSpec({
  val environment = createEnvironment()

  should("report for missing return type in public val") {
    val code = """
      class MyClass() {
        val number = 123
      }
    """.trimIndent()

    val finding = MissingTypeDeclaration().lintWithContext(environment, code).single()

    finding.message shouldBe "Missing explicit type declaration for public value 'number: Int'"
    finding.shouldStartAt(code, "val number")
  }

  should("report for missing return type in public var") {
    val code = """
      class MyClass() {
        var number = 123
      }
    """.trimIndent()

    MissingTypeDeclaration().lintWithContext(environment, code) shouldHaveSize 1
  }

  should("report for missing return type in public val with generic type") {
    val code = """
      class MyClass() {
        val myMap = mapOf<Int, String>(1 to "Many")
      }
    """.trimIndent()

    val findings = MissingTypeDeclaration().lintWithContext(environment, code)

    findings.single().message shouldBe "Missing explicit type declaration for public value 'myMap: Map<Int, String>'"
  }

  should("report for missing return type in public function") {
    val code = """
      class MyClass() {
        fun myNumberFun() = 123
      }
    """.trimIndent()

    val finding = MissingTypeDeclaration().lintWithContext(environment, code).single()

    finding.message shouldBe "Missing explicit type declaration for public function 'myNumberFun: Int'"
    finding.shouldStartAt(code, "fun myNumberFun")
  }

  should("not report for declared type in public val") {
    val code = """
      class MyClass() {
        val number: Int = 123
      }
    """.trimIndent()

    MissingTypeDeclaration().lintWithContext(environment, code).shouldBeEmpty()
  }

  should("not report for missing return type in public val inside of ignored derived interface") {
    val code = """
      interface MyInterface
      interface YourInterface
      class MyClass(): MyInterface, YourInterface {
        val intent = 123
      }
    """.trimIndent()

    val findings = MissingTypeDeclaration(
      ignoreInInterfacesDerivedFrom = listOf("YourInterface"),
    ).lintWithContext(environment, code)

    findings.shouldBeEmpty()
  }

  should("report in a class implementing an ignored interface only indirectly") {
    val code = """
      interface YourInterface
      open class ParentClass(): YourInterface
      class MyClass(): ParentClass() {
        val intent = 123
      }
    """.trimIndent()

    val findings = MissingTypeDeclaration(
      ignoreInInterfacesDerivedFrom = listOf("YourInterface"),
    ).lintWithContext(environment, code)

    findings shouldHaveSize 1
  }

  should("not do rule check inside of ignored derived class") {
    val code = """
      open class ParentClass()
      class MyChildClass(): ParentClass() {
        val intent = 123
        fun numbersFun() = 123
      }
    """.trimIndent()

    val findings = MissingTypeDeclaration(
      ignoreInClassesDerivedFrom = listOf("ParentClass"),
    ).lintWithContext(environment, code)

    findings.shouldBeEmpty()
  }

  should("not do rule check inside of derived class from ignored interface") {
    val code = """
      interface MyInterface
      open class ParentClass(): MyInterface
      class MyChildClass(): ParentClass() {
        val intent = 123
        fun numbersFun() = 123
      }
    """.trimIndent()

    val findings = MissingTypeDeclaration(
      ignoreInClassesDerivedFrom = listOf("MyInterface"),
    ).lintWithContext(environment, code)

    findings.shouldBeEmpty()
  }

  should("not report for missing a type for ignored property types") {
    val code = """
      class MyChildClass() {
        val numberVal = 123
        var numberVar = 123
      }
    """.trimIndent()

    val findings = MissingTypeDeclaration(
      ignorePropertiesOfType = listOf("Int"),
    ).lintWithContext(environment, code)

    findings.shouldBeEmpty()
  }

  should("read options from config, ignored property types as regular expressions") {
    val code = """
      class Task2<A, B>
      class MyClass() {
        val task = Task2<Int, List<String>>()
        val number = 123
      }
    """.trimIndent()
    val config = TestConfig("ignorePropertiesOfType" to listOf("^Task\\d+<(?:[^<>]*|<[^<>]*>)*>$"))

    val findings = MissingTypeDeclaration(config).lintWithContext(environment, code)

    findings.single().message shouldBe "Missing explicit type declaration for public value 'number: Int'"
  }

  should("not report for declared type in public var") {
    val code = """
      class MyClass() {
        var state: Int = 123
      }
    """.trimIndent()

    MissingTypeDeclaration().lintWithContext(environment, code).shouldBeEmpty()
  }

  should("not report for missing return type in public function with Unit return type") {
    val code = """
      class MyClass() {
        fun myUnitFunction() {}
        fun myUnitExpression() = println()
      }
    """.trimIndent()

    MissingTypeDeclaration().lintWithContext(environment, code).shouldBeEmpty()
  }

  should("not report non-public declarations") {
    val code = """
      class MyClass() {
        private val a = 1
        protected val b = 2
        internal val c = 3
        private fun d() = 4
      }
    """.trimIndent()

    MissingTypeDeclaration().lintWithContext(environment, code).shouldBeEmpty()
  }

  should("keep 1.x scope: nested classes and companions, not top level, objects or locals") {
    val code = """
      val topLevel = 1
      fun topLevelFun() = 1
      object Singleton { val inObject = 1 }
      class MyClass() {
        class Nested { val inNested = 1 }
        companion object { val inCompanion = 1 }
        fun member(): Int {
          val local = 1
          return local
        }
      }
    """.trimIndent()

    val findings = MissingTypeDeclaration().lintWithContext(environment, code)

    findings.map { it.message } shouldContainExactly listOf(
      "Missing explicit type declaration for public value 'inNested: Int'",
      "Missing explicit type declaration for public value 'inCompanion: Int'",
    )
  }
})
