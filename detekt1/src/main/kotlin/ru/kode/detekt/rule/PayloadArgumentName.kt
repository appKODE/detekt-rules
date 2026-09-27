package ru.kode.detekt.rule

import io.gitlab.arturbosch.detekt.api.Config
import org.jetbrains.kotlin.psi.KtCallExpression
import ru.kode.detekt.rule.shared.analyzer.PayloadArgumentNameAnalyzer

class PayloadArgumentName(config: Config = Config.empty) : Detekt1SharedRule(
  config,
  "Checks onEach payload argument name",
) {

  private val analyzer = PayloadArgumentNameAnalyzer()

  override fun visitCallExpression(expression: KtCallExpression) {
    reportDiagnostics(analyzer.analyze(expression))
    super.visitCallExpression(expression)
  }
}
