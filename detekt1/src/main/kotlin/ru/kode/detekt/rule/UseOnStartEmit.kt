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
    val diagnostic = analyzer.analyze(expression)
    // 1.x did not descend into a reported chain
    if (diagnostic != null) reportDiagnostics(listOf(diagnostic)) else super.visitDotQualifiedExpression(expression)
  }
}
