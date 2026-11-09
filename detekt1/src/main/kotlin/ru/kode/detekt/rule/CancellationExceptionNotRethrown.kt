package ru.kode.detekt.rule

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.config
import io.gitlab.arturbosch.detekt.api.internal.RequiresTypeResolution
import org.jetbrains.kotlin.psi.KtTryExpression
import org.jetbrains.kotlin.resolve.BindingContext
import ru.kode.detekt.rule.shared.analyzer.CancellationExceptionNotRethrownAnalyzer

@RequiresTypeResolution
class CancellationExceptionNotRethrown(config: Config = Config.empty) : Detekt1SharedRule(
  config,
  "Reports catch clauses around suspending calls that swallow CancellationException",
) {

  private val rethrowFunctions: List<String> by config(defaultValue = emptyList())

  override fun visitTryExpression(expression: KtTryExpression) {
    super.visitTryExpression(expression)
    if (bindingContext == BindingContext.EMPTY) return
    // bindingContext is set per file, so the analyzer cannot be cached
    val analyzer =
      CancellationExceptionNotRethrownAnalyzer(BindingContextKodeSemantic(bindingContext), rethrowFunctions)
    reportDiagnostics(listOfNotNull(analyzer.analyze(expression)))
  }
}
