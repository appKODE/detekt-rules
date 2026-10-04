package ru.kode.detekt.rule

import dev.detekt.test.lint
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

class SealedClassCouldBeEnumTest : ShouldSpec({
  should("report a sealed interface with nested object inheritors only") {
    val code = """
      sealed interface Direction {
        object Up : Direction
        data object Down : Direction
      }
    """.trimIndent()

    val finding = SealedClassCouldBeEnum().lint(code).single()

    finding.message shouldBe "Sealed type \"Direction\" has only object inheritors: use an enum class instead"
    finding.shouldStartAt(code, "sealed interface Direction")
  }

  should("report a sealed class with top-level object inheritors overriding its members") {
    val code = """
      sealed class Direction {
        abstract val title: String
      }
      object Up : Direction() {
        override val title = "up"
      }
      object Down : Direction() {
        override val title = "down"
      }
    """.trimIndent()

    SealedClassCouldBeEnum().lint(code).single()
  }

  should("not report when an inheritor is a class or an anonymous object") {
    val code = """
      sealed interface State {
        object Loading : State
        class Content(val items: Int) : State
      }
      sealed interface Action {
        object Open : Action
        object Close : Action
      }
      val custom = object : Action {}
    """.trimIndent()

    SealedClassCouldBeEnum().lint(code).shouldBeEmpty()
  }

  should("not report when an object has own state or another supertype") {
    val code = """
      interface Marker
      sealed interface Counter {
        object First : Counter {
          var count = 0
        }
        object Second : Counter
      }
      sealed interface Event {
        object Opened : Event, Marker
        object Closed : Event
      }
    """.trimIndent()

    SealedClassCouldBeEnum().lint(code).shouldBeEmpty()
  }

  should("not report generic sealed types, sealed types with a superclass and types with fewer than two inheritors") {
    val code = """
      open class Base
      sealed class Result<out T> {
        object Empty : Result<Nothing>()
        object Failed : Result<Nothing>()
      }
      sealed class Child : Base() {
        object One : Child()
        object Two : Child()
      }
      sealed interface Single {
        object Only : Single
      }
      sealed interface Declared
      class Holder {
        companion object : Declared
      }
      object Other : Declared
    """.trimIndent()

    SealedClassCouldBeEnum().lint(code).shouldBeEmpty()
  }

  should("not report a class that is not sealed") {
    val code = """
      interface Direction
      object Up : Direction
      object Down : Direction
    """.trimIndent()

    SealedClassCouldBeEnum().lint(code).shouldBeEmpty()
  }
})
