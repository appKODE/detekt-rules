package ru.kode.detekt.rule

import dev.detekt.api.Config
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtNamedFunction
import ru.kode.detekt.rule.shared.analyzer.BlockingSqlDelightCallAnalyzer

class BlockingSqlDelightCall(
  config: Config = Config.empty,
  sqlDelightPackage: String = "app.cash.sqldelight",
) : Rule(config, "Reports SqlDelight DB calls not made within the coroutine context"), RequiresAnalysisApi {

  private val sqlDelightPackage by config(defaultValue = sqlDelightPackage)

  private val analyzer by lazy { BlockingSqlDelightCallAnalyzer(this.sqlDelightPackage, AnalysisApiKodeSemantic) }

  override fun visitNamedFunction(function: KtNamedFunction) {
    reportDiagnostics(analyzer.analyze(function))
    super.visitNamedFunction(function)
  }
}
