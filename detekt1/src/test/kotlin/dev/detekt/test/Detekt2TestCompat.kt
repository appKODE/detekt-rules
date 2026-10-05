package dev.detekt.test

import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.test.utils.KotlinEnvironmentContainer
import org.jetbrains.kotlin.psi.KtFile
import io.gitlab.arturbosch.detekt.test.TestConfig as Detekt1TestConfig
import io.gitlab.arturbosch.detekt.test.lint as detekt1Lint
import io.gitlab.arturbosch.detekt.test.lintWithContext as detekt1LintWithContext

typealias TestConfig = Detekt1TestConfig

fun Rule.lint(code: String): List<Finding> = this.detekt1Lint(code)

fun <T : Rule> T.lintWithContext(
  environment: KotlinEnvironmentContainer,
  code: String,
  vararg dependencyCodes: String,
): List<Finding> = this.detekt1LintWithContext(environment.env, code, *dependencyCodes)

fun Rule.lint(ktFile: KtFile): List<Finding> = this.detekt1Lint(ktFile)
