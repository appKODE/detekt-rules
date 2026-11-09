package ru.kode.detekt.rule

import dev.detekt.api.Config
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import ru.kode.detekt.rule.shared.analyzer.ConstantsNotAtBottomAnalyzer

class ConstantsNotAtBottom(config: Config = Config.empty) : Rule(
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
