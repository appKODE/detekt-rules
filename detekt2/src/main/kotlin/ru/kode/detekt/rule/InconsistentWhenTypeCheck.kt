package ru.kode.detekt.rule

import dev.detekt.api.Config
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtWhenExpression
import ru.kode.detekt.rule.shared.analyzer.InconsistentWhenTypeCheckAnalyzer

class InconsistentWhenTypeCheck(config: Config = Config.empty) : Rule(
  config,
  "Reports \"when\" expressions mixing \"is\" checks with equality checks of objects",
) {

  private val analyzer = InconsistentWhenTypeCheckAnalyzer()

  override fun visitWhenExpression(expression: KtWhenExpression) {
    reportDiagnostics(analyzer.analyze(expression))
    super.visitWhenExpression(expression)
  }
}
