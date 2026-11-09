package ru.kode.detekt.rule

import dev.detekt.test.TestConfig
import dev.detekt.test.lintWithContext
import dev.detekt.test.utils.createEnvironment
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class PublicSuspendFunctionInReactiveModelTest : ShouldSpec({
  val environment = createEnvironment()

  should("report a public suspend function of a reactive model") {
    val code = """
      open class ReactiveModel

      class ProfileModel : ReactiveModel() {
        suspend fun load(): Int = 1
      }
    """.trimIndent()

    val finding = PublicSuspendFunctionInReactiveModel().lintWithContext(environment, code).single()

    finding.message shouldBe "Reactive model \"ProfileModel\" should not expose public suspend function \"load\": " +
      "make it a task or make it private"
    finding.shouldStartAt(code, "suspend fun load")
  }

  should("report in indirect inheritors and in nested models") {
    val code = """
      open class ReactiveModel
      abstract class BaseModel : ReactiveModel()

      class ProfileModel : BaseModel() {
        public suspend fun load(): Int = 1

        class Nested : ReactiveModel() {
          suspend fun reload(): Int = 2
        }
      }
    """.trimIndent()

    PublicSuspendFunctionInReactiveModel().lintWithContext(environment, code) shouldHaveSize 2
  }

  should("not report non-public, overridden and regular functions") {
    val code = """
      interface Loader {
        suspend fun fetch(): Int
      }
      open class ReactiveModel

      class ProfileModel : ReactiveModel(), Loader {
        private suspend fun load(): Int = 1
        internal suspend fun reload(): Int = 2
        protected suspend fun refresh(): Int = 3
        override suspend fun fetch(): Int = 4
        fun start(): Int = 5

        fun launch() {
          suspend fun local(): Int = 6
        }
      }
    """.trimIndent()

    PublicSuspendFunctionInReactiveModel().lintWithContext(environment, code).shouldBeEmpty()
  }

  should("not report the base class itself and classes that are not reactive models") {
    val code = """
      open class ReactiveModel {
        suspend fun await(): Int = 1
      }

      class Repository {
        suspend fun load(): Int = 2
      }
    """.trimIndent()

    PublicSuspendFunctionInReactiveModel().lintWithContext(environment, code).shouldBeEmpty()
  }

  should("use the configured base classes") {
    val code = """
      open class Store

      class ProfileStore : Store() {
        suspend fun load(): Int = 1
      }
    """.trimIndent()

    PublicSuspendFunctionInReactiveModel().lintWithContext(environment, code).shouldBeEmpty()
    val config = TestConfig("baseClasses" to listOf("Store"))
    PublicSuspendFunctionInReactiveModel(config).lintWithContext(environment, code) shouldHaveSize 1
  }
})
