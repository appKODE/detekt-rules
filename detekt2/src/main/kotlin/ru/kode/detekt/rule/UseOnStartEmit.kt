package ru.kode.detekt.rule

import dev.detekt.api.Config
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import ru.kode.detekt.rule.shared.analyzer.UseOnStartEmitAnalyzer

class UseOnStartEmit(config: Config = Config.empty) : Rule(
  config,
  "Suggests using Flow.onStartEmit instead of onStart + emit",
) {

  private val analyzer = UseOnStartEmitAnalyzer()

  override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
    reportDiagnostics(analyzer.analyze(expression))
    super.visitDotQualifiedExpression(expression)
  }
}
