package ru.kode.detekt.rule

import dev.detekt.test.lint
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class RouteWiringMethodNamingTest : ShouldSpec({
  should("report error for incorrect naming inside wiring class") {
    val code = """
      class SomeWiring(private val coordinator: SomeCoordinator) {
        fun openUserDetails() {
          coordinator.handleEvent(Event.OpenUserDetailsRequested)
        }

        fun openUserProfile() {
          coordinator.handleEvent(Event.OpenUserProfile)
        }
      }
    """.trimIndent()

    val findings = RouteWiringMethodNaming().lint(code)

    findings shouldHaveSize 2
    findings[0].message shouldBe "Use \"navigateOn\" prefix for routing-related function names inside a Wiring class"
    findings[0].shouldStartAt(code, "fun openUserDetails")
    findings[1].shouldStartAt(code, "fun openUserProfile")
  }

  should("report error for incorrect naming inside WiringImpl class name") {
    val code = """
      class SomeWiringImpl(private val coordinator: SomeCoordinator) {
        fun openUserDetails() {
          coordinator.handleEvent(Event.OpenUserDetailsRequested)
        }
      }
    """.trimIndent()

    RouteWiringMethodNaming().lint(code) shouldHaveSize 1
  }

  should("return success when naming is correct") {
    val code = """
      class SomeWiring(private val coordinator: SomeCoordinator) {
        fun navigateOnOpenUserDetails() {
          coordinator.handleEvent(Event.OpenUserDetailsRequested)
        }

        fun navigateOnOpenUserProfile() {
          coordinator.handleEvent(Event.OpenUserProfile)
        }
      }
    """.trimIndent()

    RouteWiringMethodNaming().lint(code).shouldBeEmpty()
  }

  should("return success when methods do not contain handleEvent calls") {
    val code = """
      class SomeWiring(private val coordinator: SomeCoordinator) {
        fun doSomething() {
          println("hello")
        }
      }
    """.trimIndent()

    RouteWiringMethodNaming().lint(code).shouldBeEmpty()
  }

  should("return success when method named handleEvent is called outside of wiring class") {
    val code = """
      class SomeModel(private val coordinator: SomeCoordinator) {
        fun doSomething() {
          coordinator.handleEvent(Event.OpenUserProfile)
        }
      }

      fun topLevel() {
        coordinator.handleEvent(Event.OpenUserProfile)
      }
    """.trimIndent()

    RouteWiringMethodNaming().lint(code).shouldBeEmpty()
  }

  should("match the handleEvent call by text, expression bodies and any coordinator name included") {
    val code = """
      class SomeWiring(private val flowCoordinator: SomeCoordinator) {
        fun open() = flowCoordinator.handleEvent(Event.Open)
        fun close() {
          // Coordinator.handleEvent in a comment counts too
        }
        fun other() {
          router.handleEvent(Event.Open)
        }
      }
    """.trimIndent()

    val findings = RouteWiringMethodNaming().lint(code)

    findings shouldHaveSize 2
    findings[0].shouldStartAt(code, "fun open")
    findings[1].shouldStartAt(code, "fun close")
  }

  should("report functions of objects and local functions inside a wiring class") {
    val code = """
      class SomeWiring {
        companion object {
          fun open() { coordinator.handleEvent(Event.Open) }
        }
        fun navigateOnWrapper() {
          fun local() { coordinator.handleEvent(Event.Open) }
        }
      }
    """.trimIndent()

    val findings = RouteWiringMethodNaming().lint(code)

    findings shouldHaveSize 2
    findings[0].shouldStartAt(code, "fun open")
    findings[1].shouldStartAt(code, "fun local")
  }

  should("check functions of a nested wiring class against its own name") {
    val code = """
      class Screen {
        class NestedWiring {
          fun open() { coordinator.handleEvent(Event.Open) }
        }
      }
    """.trimIndent()

    RouteWiringMethodNaming().lint(code).single().shouldStartAt(code, "fun open")
  }

  // 1.x quirk: the "inside a Wiring class" flag was cleared when leaving any class, so everything checked after a
  // nested (or local) class inside the wiring class is skipped
  should("skip functions checked after a nested or local class of a wiring class") {
    val code = """
      class SomeWiring {
        fun first() { coordinator.handleEvent(Event.Open) }
        class Nested
        fun afterNested() { coordinator.handleEvent(Event.Open) }
      }

      class OtherWiring {
        fun withLocalClass() {
          class Local
          coordinator.handleEvent(Event.Open)
        }
      }

      enum class ThirdWiring {
        ENTRY;
        fun afterEntry() { coordinator.handleEvent(Event.Open) }
      }
    """.trimIndent()

    RouteWiringMethodNaming().lint(code).single().shouldStartAt(code, "fun first")
  }
})
