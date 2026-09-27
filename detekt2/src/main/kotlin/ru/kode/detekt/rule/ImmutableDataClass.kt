package ru.kode.detekt.rule

import dev.detekt.api.Config
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtClass
import ru.kode.detekt.rule.shared.analyzer.ImmutableDataClassAnalyzer

class ImmutableDataClass(
  config: Config = Config.empty,
  ignoreDescendantsOf: List<String> = emptyList(),
) : Rule(config, "Reports missing @Immutable annotations in ui packages") {

  private val ignoreDescendantsOf by config(defaultValue = ignoreDescendantsOf)

  private val analyzer by lazy { ImmutableDataClassAnalyzer(this.ignoreDescendantsOf) }

  override fun visitClass(klass: KtClass) {
    super.visitClass(klass)
    reportDiagnostics(listOfNotNull(analyzer.analyze(klass)))
  }
}
