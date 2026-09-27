package ru.kode.detekt.rule

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.config
import org.jetbrains.kotlin.psi.KtClass
import ru.kode.detekt.rule.shared.analyzer.ImmutableDataClassAnalyzer

class ImmutableDataClass(
  config: Config = Config.empty,
  ignoreDescendantsOf: List<String> = emptyList(),
) : Detekt1SharedRule(config, "Reports missing @Immutable annotations in ui packages") {

  private val ignoreDescendantsOf by config(defaultValue = ignoreDescendantsOf)

  private val analyzer by lazy { ImmutableDataClassAnalyzer(this.ignoreDescendantsOf) }

  override fun visitClass(klass: KtClass) {
    super.visitClass(klass)
    reportDiagnostics(listOfNotNull(analyzer.analyze(klass)))
  }
}
