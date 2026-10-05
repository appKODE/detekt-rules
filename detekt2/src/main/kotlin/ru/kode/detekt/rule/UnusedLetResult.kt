package ru.kode.detekt.rule

import dev.detekt.api.Config
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtCallExpression
import ru.kode.detekt.rule.shared.analyzer.UnusedLetResultAnalyzer

class UnusedLetResult(config: Config = Config.empty) :
  Rule(
    config,
    "Reports \"let\" calls whose result is not used",
  ),
  RequiresAnalysisApi {

  private val analyzer = UnusedLetResultAnalyzer(AnalysisApiKodeSemantic)

  override fun visitCallExpression(expression: KtCallExpression) {
    reportDiagnostics(listOfNotNull(analyzer.analyze(expression)))
    super.visitCallExpression(expression)
  }
}
