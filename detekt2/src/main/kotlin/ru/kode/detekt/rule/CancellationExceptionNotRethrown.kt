package ru.kode.detekt.rule

import dev.detekt.api.Config
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtNamedFunction
import ru.kode.detekt.rule.shared.analyzer.CancellationExceptionNotRethrownAnalyzer

class CancellationExceptionNotRethrown(config: Config = Config.empty) : Rule(
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
