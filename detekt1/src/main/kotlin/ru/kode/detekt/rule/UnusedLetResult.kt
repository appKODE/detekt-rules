package ru.kode.detekt.rule

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.internal.RequiresTypeResolution
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.resolve.BindingContext
import ru.kode.detekt.rule.shared.analyzer.UnusedLetResultAnalyzer

@RequiresTypeResolution
class UnusedLetResult(config: Config = Config.empty) : Detekt1SharedRule(
  config,
  "Reports \"let\" calls whose result is not used",
) {

  override fun visitCallExpression(expression: KtCallExpression) {
    super.visitCallExpression(expression)
    if (bindingContext == BindingContext.EMPTY) return
    // bindingContext is set per file, so the analyzer cannot be cached
    val analyzer = UnusedLetResultAnalyzer(BindingContextKodeSemantic(bindingContext))
    reportDiagnostics(listOfNotNull(analyzer.analyze(expression)))
  }
}
