package ru.kode.detekt.rule

import dev.detekt.test.lintWithContext
import dev.detekt.test.utils.createEnvironment
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class SealedClassCouldBeEnumTest : ShouldSpec({
  val environment = createEnvironment()

  should("report a sealed interface with nested object inheritors only") {
    val code = """
      sealed interface Direction {
        object Up : Direction
        data object Down : Direction
      }
    """.trimIndent()

    val finding = SealedClassCouldBeEnum().lintWithContext(environment, code).single()

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

    SealedClassCouldBeEnum().lintWithContext(environment, code).single()
  }

  should("not report when an inheritor is a class") {
    val code = """
      sealed interface State {
        object Loading : State
        class Content(val items: Int) : State
      }
    """.trimIndent()

    SealedClassCouldBeEnum().lintWithContext(environment, code).shouldBeEmpty()
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

    SealedClassCouldBeEnum().lintWithContext(environment, code).shouldBeEmpty()
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

    SealedClassCouldBeEnum().lintWithContext(environment, code).shouldBeEmpty()
  }

  should("tell same-named sealed types apart") {
    val code = """
      class First {
        sealed interface State {
          object Loading : State
          object Ready : State
        }
      }

      class Second {
        sealed class State {
          class Content(val items: List<String>) : State()
          object Empty : State()
        }
      }
    """.trimIndent()

    SealedClassCouldBeEnum().lintWithContext(environment, code).single().shouldStartAt(code, "sealed interface State")
  }

  should("count the inheritors declared in other files") {
    val code = """
      package shapes

      sealed interface Direction {
        object Up : Direction
      }
      sealed interface Shape {
        object Circle : Shape
        object Dot : Shape
      }
    """.trimIndent()
    val other = """
      package shapes

      object Down : Direction
      class Square(val side: Int) : Shape
    """.trimIndent()

    SealedClassCouldBeEnum().lintWithContext(environment, code, other).map { it.message }.shouldContainExactly(
      "Sealed type \"Direction\" has only object inheritors: use an enum class instead",
    )
  }

  should("not report a class that is not sealed") {
    val code = """
      interface Direction
      object Up : Direction
      object Down : Direction
    """.trimIndent()

    SealedClassCouldBeEnum().lintWithContext(environment, code).shouldBeEmpty()
  }
})
