package ru.kode.detekt.rule

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.config
import io.gitlab.arturbosch.detekt.api.internal.RequiresTypeResolution
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.resolve.BindingContext
import ru.kode.detekt.rule.shared.analyzer.PublicSuspendFunctionInReactiveModelAnalyzer

@RequiresTypeResolution
class PublicSuspendFunctionInReactiveModel(
  config: Config = Config.empty,
) : Detekt1SharedRule(config, "Reports public suspend functions of reactive models") {

  private val baseClasses: List<String> by config(defaultValue = listOf("ReactiveModel"))

  override fun visitClass(klass: KtClass) {
    super.visitClass(klass)
    if (bindingContext == BindingContext.EMPTY) return
    // bindingContext is set per file, so the analyzer cannot be cached
    val analyzer = PublicSuspendFunctionInReactiveModelAnalyzer(baseClasses, BindingContextKodeSemantic(bindingContext))
    reportDiagnostics(analyzer.analyze(klass))
  }
}
