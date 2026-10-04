package ru.kode.detekt.rule

import io.gitlab.arturbosch.detekt.api.Config
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import ru.kode.detekt.rule.shared.analyzer.ConstantsNotAtBottomAnalyzer

class ConstantsNotAtBottom(config: Config = Config.empty) : Detekt1SharedRule(
  config,
  "Reports constants declared above the code that uses them",
) {

  private val analyzer = ConstantsNotAtBottomAnalyzer()

  override fun visitKtFile(file: KtFile) {
    reportDiagnostics(listOfNotNull(analyzer.analyze(file)))
    super.visitKtFile(file)
  }

  override fun visitClass(klass: KtClass) {
    super.visitClass(klass)
    reportDiagnostics(listOfNotNull(analyzer.analyze(klass)))
  }
}
