package ru.kode.detekt.rule

import io.gitlab.arturbosch.detekt.api.Config
import org.jetbrains.kotlin.psi.KtClass
import ru.kode.detekt.rule.shared.analyzer.NullableValueClassPropertyAnalyzer

class NullableValueClassProperty(config: Config = Config.empty) : Detekt1SharedRule(
  config,
  "Reports value classes wrapping a nullable type",
) {

  private val analyzer = NullableValueClassPropertyAnalyzer()

  override fun visitClass(klass: KtClass) {
    super.visitClass(klass)
    reportDiagnostics(listOfNotNull(analyzer.analyze(klass)))
  }
}
