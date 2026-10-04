package ru.kode.detekt.rule

import dev.detekt.api.Config
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtCallExpression
import ru.kode.detekt.rule.shared.analyzer.UnusedLetResultAnalyzer

class UnusedLetResult(config: Config = Config.empty) : Rule(
  config,
  "Reports \"let\" calls whose result is not used",
) {

  private val analyzer = UnusedLetResultAnalyzer()

  override fun visitCallExpression(expression: KtCallExpression) {
    reportDiagnostics(listOfNotNull(analyzer.analyze(expression)))
    super.visitCallExpression(expression)
  }
}
