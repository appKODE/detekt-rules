package ru.kode.detekt.rule

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.config
import org.jetbrains.kotlin.psi.KtNamedFunction
import ru.kode.detekt.rule.shared.analyzer.CancellationExceptionNotRethrownAnalyzer

class CancellationExceptionNotRethrown(config: Config = Config.empty) : Detekt1SharedRule(
  config,
  "Reports catch clauses of suspend functions that swallow CancellationException",
) {

  private val rethrowFunctions: List<String> by config(defaultValue = emptyList())

  private val analyzer by lazy { CancellationExceptionNotRethrownAnalyzer(this.rethrowFunctions) }

  override fun visitNamedFunction(function: KtNamedFunction) {
    reportDiagnostics(analyzer.analyze(function))
    super.visitNamedFunction(function)
  }
}
