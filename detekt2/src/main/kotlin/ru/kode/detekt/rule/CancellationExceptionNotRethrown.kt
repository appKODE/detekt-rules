package ru.kode.detekt.rule

import dev.detekt.api.Config
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtTryExpression
import ru.kode.detekt.rule.shared.analyzer.CancellationExceptionNotRethrownAnalyzer

class CancellationExceptionNotRethrown(config: Config = Config.empty) :
  Rule(
    config,
    "Reports catch clauses around suspending calls that swallow CancellationException",
  ),
  RequiresAnalysisApi {

  private val rethrowFunctions: List<String> by config(defaultValue = emptyList())

  private val analyzer by lazy {
    CancellationExceptionNotRethrownAnalyzer(AnalysisApiKodeSemantic, this.rethrowFunctions)
  }

  override fun visitTryExpression(expression: KtTryExpression) {
    reportDiagnostics(listOfNotNull(analyzer.analyze(expression)))
    super.visitTryExpression(expression)
  }
}
