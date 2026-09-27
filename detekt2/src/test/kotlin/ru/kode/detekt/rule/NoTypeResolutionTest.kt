package ru.kode.detekt.rule

import dev.detekt.test.lint
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.string.shouldContain

/**
 * detekt2 has no "empty context" mode: `lint` refuses RequiresAnalysisApi rules up front, so these rules can only
 * run with an Analysis API session.
 */
class NoTypeResolutionTest : ShouldSpec({
  should("refuse to lint without Analysis API: MissingTypeDeclaration") {
    val error = shouldThrow<IllegalArgumentException> {
      MissingTypeDeclaration().lint("class MyClass() { val number = 123 }")
    }

    error.message shouldContain "requires Analysis API"
  }

  should("refuse to lint without Analysis API: BlockingSqlDelightCall") {
    val error = shouldThrow<IllegalArgumentException> {
      BlockingSqlDelightCall().lint("suspend fun run() {}")
    }

    error.message shouldContain "requires Analysis API"
  }
})
