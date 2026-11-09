package ru.kode.detekt.rule

import dev.detekt.test.lint
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

class ConstantsNotAtBottomTest : ShouldSpec({
  should("report a top-level constant followed by a class") {
    val code = """
      private const val PAGE_SIZE = 20

      class Pager
    """.trimIndent()

    val finding = ConstantsNotAtBottom().lint(code).single()

    finding.message shouldBe "Constant \"PAGE_SIZE\" should be declared at the bottom of the file"
    finding.shouldStartAt(code, "private const")
  }

  should("report only the first misplaced top-level constant") {
    val code = """
      fun first() = Unit
      const val FIRST = 1
      const val SECOND = 2
      fun last() = Unit
      const val THIRD = 3
    """.trimIndent()

    ConstantsNotAtBottom().lint(code).single().shouldStartAt(code, "const val FIRST")
  }

  should("not report top-level constants at the bottom of the file") {
    val code = """
      class Pager

      fun load() = Unit

      private const val PAGE_SIZE = 20
      private val DEFAULT_PAGES = listOf(1)
      const val FIRST_PAGE = 0
    """.trimIndent()

    ConstantsNotAtBottom().lint(code).shouldBeEmpty()
  }

  should("not report a file of constants and properties only") {
    val code = """
      const val FIRST = 1
      val names = listOf("a")
      const val SECOND = 2
    """.trimIndent()

    ConstantsNotAtBottom().lint(code).shouldBeEmpty()
  }

  should("not report a non-constant property followed by a class, or an empty file") {
    ConstantsNotAtBottom().lint("val names = listOf(1)\nclass Pager").shouldBeEmpty()
    ConstantsNotAtBottom().lint("").shouldBeEmpty()
  }

  should("report a companion object with constants that is not the last class member") {
    val code = """
      class Pager {
        companion object {
          const val FIRST_PAGE = 0
        }

        fun load() = Unit
      }
    """.trimIndent()

    val finding = ConstantsNotAtBottom().lint(code).single()

    finding.message shouldBe "Companion object with constants should be the last declaration of class \"Pager\""
    finding.shouldStartAt(code, "companion object")
  }

  should("report a misplaced companion object of a nested class") {
    val code = """
      class Outer {
        interface Inner {
          companion object { const val KEY = "key" }
          val value: Int
        }
      }
    """.trimIndent()

    ConstantsNotAtBottom().lint(code).single().shouldStartAt(code, "companion object")
  }

  should("not report a companion object with constants at the bottom of the class") {
    val code = """
      enum class Mode {
        Fast, Slow;

        fun load() = Unit

        companion object {
          const val FIRST_PAGE = 0
        }
      }
    """.trimIndent()

    ConstantsNotAtBottom().lint(code).shouldBeEmpty()
  }

  should("not report a companion object without constants, or a class without a companion") {
    val code = """
      class Pager {
        companion object {
          fun create() = Pager()
          val pages = listOf(1)
        }

        fun load() = Unit
      }

      class Plain {
        object Nested { const val KEY = 1 }
        fun load() = Unit
      }
    """.trimIndent()

    ConstantsNotAtBottom().lint(code).shouldBeEmpty()
  }
})
