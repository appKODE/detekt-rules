package ru.kode.detekt.rule

import io.gitlab.arturbosch.detekt.test.lint
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldBeEmpty

/**
 * detekt1 lets `lint` run type-resolution rules with an empty BindingContext (detekt-core itself skips them when
 * there is no classpath). MissingTypeDeclaration and
 * BlockingSqlDelightCall must then report nothing, as 1.x did.
 */
class NoTypeResolutionTest : ShouldSpec({
  should("skip the check without type resolution: MissingTypeDeclaration") {
    val code = """
      class MyClass() {
        val number = 123
      }
    """.trimIndent()

    MissingTypeDeclaration().lint(code).shouldBeEmpty()
  }

  should("skip the check without type resolution: BlockingSqlDelightCall") {
    val code = """
      package app.cash.sqldelight

      interface Transacter { fun transaction() }

      suspend fun run(db: Transacter) {
        db.transaction()
      }
    """.trimIndent()

    BlockingSqlDelightCall().lint(code).shouldBeEmpty()
  }
})
