package ru.kode.detekt.rule

import dev.detekt.api.Config
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtClass
import ru.kode.detekt.rule.shared.analyzer.UnhandledViewIntentAnalyzer

class UnhandledViewIntent(config: Config = Config.empty) :
  Rule(
    config,
    "Reports view intents that are never handled in the machine of a view model",
  ),
  RequiresAnalysisApi {

  private val analyzer = UnhandledViewIntentAnalyzer(AnalysisApiKodeSemantic)

  override fun visitClass(klass: KtClass) {
    super.visitClass(klass)
    reportDiagnostics(listOfNotNull(analyzer.analyze(klass)))
  }
}
