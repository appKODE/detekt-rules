package ru.kode.detekt.rule

import io.gitlab.arturbosch.detekt.api.Config
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import ru.kode.detekt.rule.shared.analyzer.ComponentFunctionCallAnalyzer

class ComponentFunctionCall(config: Config = Config.empty) : Detekt1SharedRule(
  config,
  "Reports usage of \"componentN\" functions",
) {

  private val analyzer = ComponentFunctionCallAnalyzer()

  override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
    reportDiagnostics(analyzer.analyze(expression))
    super.visitDotQualifiedExpression(expression)
  }
}
