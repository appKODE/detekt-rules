package ru.kode.detekt.rule

import io.gitlab.arturbosch.detekt.api.Config
import org.jetbrains.kotlin.psi.KtCallExpression
import ru.kode.detekt.rule.shared.analyzer.UnusedLetResultAnalyzer

class UnusedLetResult(config: Config = Config.empty) : Detekt1SharedRule(
  config,
  "Reports \"let\" calls whose result is not used",
) {

  private val analyzer = UnusedLetResultAnalyzer()

  override fun visitCallExpression(expression: KtCallExpression) {
    reportDiagnostics(listOfNotNull(analyzer.analyze(expression)))
    super.visitCallExpression(expression)
  }
}
