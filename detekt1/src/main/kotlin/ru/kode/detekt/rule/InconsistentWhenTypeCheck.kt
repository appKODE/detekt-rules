package ru.kode.detekt.rule

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.internal.RequiresTypeResolution
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.resolve.BindingContext
import ru.kode.detekt.rule.shared.analyzer.InconsistentWhenTypeCheckAnalyzer

@RequiresTypeResolution
class InconsistentWhenTypeCheck(config: Config = Config.empty) : Detekt1SharedRule(
  config,
  "Reports \"when\" expressions mixing \"is\" checks with equality checks of objects",
) {

  override fun visitWhenExpression(expression: KtWhenExpression) {
    super.visitWhenExpression(expression)
    if (bindingContext == BindingContext.EMPTY) return
    // bindingContext is set per file, so the analyzer cannot be cached
    val analyzer = InconsistentWhenTypeCheckAnalyzer(BindingContextKodeSemantic(bindingContext))
    reportDiagnostics(analyzer.analyze(expression))
  }
}
