package ru.kode.detekt.rule

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.internal.RequiresTypeResolution
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.resolve.BindingContext
import ru.kode.detekt.rule.shared.analyzer.UnhandledViewIntentAnalyzer

@RequiresTypeResolution
class UnhandledViewIntent(
  config: Config = Config.empty,
) : Detekt1SharedRule(config, "Reports view intents that are never handled in the machine of a view model") {

  override fun visitClass(klass: KtClass) {
    super.visitClass(klass)
    if (bindingContext == BindingContext.EMPTY) return
    // bindingContext is set per file, so the analyzer cannot be cached
    val analyzer = UnhandledViewIntentAnalyzer(BindingContextKodeSemantic(bindingContext))
    reportDiagnostics(listOfNotNull(analyzer.analyze(klass)))
  }
}
