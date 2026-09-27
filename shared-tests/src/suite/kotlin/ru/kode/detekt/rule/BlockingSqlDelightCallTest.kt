package ru.kode.detekt.rule

import dev.detekt.test.TestConfig
import dev.detekt.test.lintWithContext
import dev.detekt.test.utils.createEnvironment
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

private const val MESSAGE =
  "Database queries/transactions should be wrapped in \"withContext(Dispatchers.IO)\" to prevent blocking"

class BlockingSqlDelightCallTest : ShouldSpec({
  val environment = createEnvironment()

  fun lint(code: String) = createRule().lintWithContext(environment, code)

  should("report error for usage without context") {
    // language=kotlin
    val code = """
      package ru.kode.detekt.rules

      interface Transacter

      abstract class TransacterImpl : Transacter

      class ProductQueries : TransacterImpl() {
        fun selectAll() = Unit
      }

      interface MyDatabase : Transacter {
        val productQueries: ProductQueries
      }

      class Repository(val database: MyDatabase) {
        suspend fun runQuery() {
          database.productQueries.selectAll()
        }
      }
    """.trimIndent()

    val finding = lint(code).single()

    finding.message shouldBe MESSAGE
    finding.shouldStartAt(code, "selectAll()\n")
  }

  should("report error for calling db.transaction without context") {
    // language=kotlin
    val code = """
      package ru.kode.detekt.rules

      interface Transacter {
        fun transaction(body: () -> Unit)
      }

      interface ProductQueries : Transacter {
        fun selectAll()
      }

      interface MyDatabase : Transacter {
        val productQueries: ProductQueries
      }

      class Repository(val database: MyDatabase) {
        suspend fun runQuery() {
          database.transaction {
            println("hello")
          }
        }
      }
    """.trimIndent()

    val finding = lint(code).single()

    finding.message shouldBe MESSAGE
    finding.shouldStartAt(code, "transaction {")
  }

  should("report no error for usage with context") {
    // language=kotlin
    val code = """
      package ru.kode.detekt.rules

      object Dispatchers {
        object IO
      }

      suspend fun withContext(d: Any, body: () -> Unit) {

      }

      interface Transacter

      interface ProductQueries : Transacter {
        fun selectAll()
      }

      interface MyDatabase : Transacter {
        val productQueries: ProductQueries
      }

      class Repository(val database: MyDatabase) {
        suspend fun runQuery() {
          withContext(Dispatchers.IO) {
            database.productQueries.selectAll()
          }
        }
      }
    """.trimIndent()

    lint(code).shouldBeEmpty()
  }

  should("report no error when query is run from a non-suspending function") {
    // language=kotlin
    val code = """
      package ru.kode.detekt.rules

      object Dispatchers {
        object IO
      }

      suspend fun withContext(d: Any, body: () -> Unit) {

      }

      interface Transacter

      interface ProductQueries : Transacter {
        fun selectAll()
        fun selectOne()
      }

      interface MyDatabase : Transacter {
        val productQueries: ProductQueries
      }

      class Repository(val database: MyDatabase) {
        suspend fun runQuery() {
          withContext(Dispatchers.IO) {
            helper()
          }
        }

        // no error for this function
        fun helper() {
            database.productQueries.selectAll()
            database.productQueries.selectOne()
        }

      }
    """.trimIndent()

    lint(code).shouldBeEmpty()
  }

  should("read the package from the config and default to app.cash.sqldelight") {
    val code = "$PREAMBLE\nsuspend fun run(q: Queries) { q.select() }"

    BlockingSqlDelightCall(TestConfig("sqlDelightPackage" to "ru.kode.detekt.rules"))
      .lintWithContext(environment, code) shouldHaveSize 1
    BlockingSqlDelightCall().lintWithContext(environment, code).shouldBeEmpty()
  }

  should("report calls in lambdas and ignore calls of non-Transacter members") {
    val code = "$PREAMBLE\nsuspend fun run(q: Queries) { listOf(1).forEach { q.select() }; \"a\".trim(); println(q) }"

    val finding = lint(code).single()

    finding.shouldStartAt(code, "select() };")
  }

  should("see a withContext enclosing a local function") {
    val code = "$PREAMBLE\nsuspend fun run(q: Queries) { withContext { fun local() { q.select() } } }"

    lint(code).shouldBeEmpty()
  }

  should("see a withContext enclosing a nested suspend function") {
    val code = "$PREAMBLE\nsuspend fun run(q: Queries) { withContext { suspend fun local() { q.select() } } }"

    lint(code).shouldBeEmpty()
  }

  should("report a nested suspend function's calls once") {
    val code = "$PREAMBLE\nsuspend fun run(q: Queries) { suspend fun local() { q.select() } }"

    lint(code).single().shouldStartAt(code, "select() } }")
  }

  // a local function can only be called from the enclosing suspend function, where a blocking call blocks too
  should("check non-suspend local functions of a suspend function") {
    val code = "$PREAMBLE\nsuspend fun run(q: Queries) { fun local() { q.select() } }"

    lint(code).single().shouldStartAt(code, "select() } }")
  }

  should("not report constructor calls and Any members called on Transacter subtypes") {
    val code = "$PREAMBLE\nsuspend fun run(q: Queries) { QueriesImpl(); q.toString(); q.hashCode(); q.equals(q) }"

    lint(code).shouldBeEmpty()
  }

  should("report an override of a member declared outside Transacter, like 1.x") {
    val code = "$PREAMBLE\ninterface Dao { fun all(): Int }\n" +
      "class DaoQueries : Transacter, Dao { override fun all() = 0 }\n" +
      "suspend fun run(q: DaoQueries) { q.all() }"

    lint(code).single().shouldStartAt(code, "all() }")
  }

  should("report calls that resolve with errors, like 1.x") {
    val code = "$PREAMBLE\nsuspend fun run(q: Queries) { q.select(1) }"

    lint(code).single().shouldStartAt(code, "select(1)")
  }
})

// language=kotlin
private val PREAMBLE =
  """
    package ru.kode.detekt.rules

    interface Transacter
    interface Queries : Transacter { fun select() }
    class QueriesImpl : Queries { override fun select() {} }
    fun <T> withContext(body: () -> T): T = body()
  """.trimIndent()

private fun createRule(): BlockingSqlDelightCall = BlockingSqlDelightCall(sqlDelightPackage = "ru.kode.detekt.rules")
