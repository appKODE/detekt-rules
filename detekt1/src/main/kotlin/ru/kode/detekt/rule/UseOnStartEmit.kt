package ru.kode.detekt.rule

import io.gitlab.arturbosch.detekt.api.Config
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import ru.kode.detekt.rule.shared.analyzer.UseOnStartEmitAnalyzer

class UseOnStartEmit(config: Config = Config.empty) : Detekt1SharedRule(
  config,
  "Suggests using Flow.onStartEmit instead of onStart + emit",
) {

  private val analyzer = UseOnStartEmitAnalyzer()

  override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
    reportDiagnostics(analyzer.analyze(expression))
    super.visitDotQualifiedExpression(expression)
  }
}
