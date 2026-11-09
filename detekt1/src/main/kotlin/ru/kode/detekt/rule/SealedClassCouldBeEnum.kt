package ru.kode.detekt.rule

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.internal.RequiresTypeResolution
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.resolve.BindingContext
import ru.kode.detekt.rule.shared.analyzer.SealedClassCouldBeEnumAnalyzer

@RequiresTypeResolution
class SealedClassCouldBeEnum(config: Config = Config.empty) : Detekt1SharedRule(
  config,
  "Reports sealed types whose inheritors are all objects",
) {

  override fun visitClass(klass: KtClass) {
    super.visitClass(klass)
    if (bindingContext == BindingContext.EMPTY) return
    // bindingContext is set per file, so the analyzer cannot be cached
    val analyzer = SealedClassCouldBeEnumAnalyzer(BindingContextKodeSemantic(bindingContext))
    reportDiagnostics(listOfNotNull(analyzer.analyze(klass)))
  }
}
