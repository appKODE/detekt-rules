package ru.kode.detekt.rule

import dev.detekt.api.Config
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtClass
import ru.kode.detekt.rule.shared.analyzer.SealedClassCouldBeEnumAnalyzer

class SealedClassCouldBeEnum(config: Config = Config.empty) : Rule(
  config,
  "Reports sealed types whose inheritors are all objects",
) {

  private val analyzer = SealedClassCouldBeEnumAnalyzer()

  override fun visitClass(klass: KtClass) {
    reportDiagnostics(listOfNotNull(analyzer.analyze(klass)))
    super.visitClass(klass)
  }
}
