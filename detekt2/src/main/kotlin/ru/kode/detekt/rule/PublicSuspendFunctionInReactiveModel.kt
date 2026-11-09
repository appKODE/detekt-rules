package ru.kode.detekt.rule

import dev.detekt.api.Config
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtClass
import ru.kode.detekt.rule.shared.analyzer.PublicSuspendFunctionInReactiveModelAnalyzer

class PublicSuspendFunctionInReactiveModel(config: Config = Config.empty) :
  Rule(
    config,
    "Reports public suspend functions of reactive models",
  ),
  RequiresAnalysisApi {

  private val baseClasses: List<String> by config(defaultValue = listOf("ReactiveModel"))

  private val analyzer by lazy {
    PublicSuspendFunctionInReactiveModelAnalyzer(this.baseClasses, AnalysisApiKodeSemantic)
  }

  override fun visitClass(klass: KtClass) {
    super.visitClass(klass)
    reportDiagnostics(analyzer.analyze(klass))
  }
}
