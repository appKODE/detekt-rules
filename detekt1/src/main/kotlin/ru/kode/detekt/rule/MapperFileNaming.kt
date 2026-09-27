package ru.kode.detekt.rule

import io.gitlab.arturbosch.detekt.api.Config
import org.jetbrains.kotlin.psi.KtFile
import ru.kode.detekt.rule.shared.analyzer.MapperFileNamingAnalyzer

class MapperFileNaming(config: Config = Config.empty) : Detekt1SharedRule(
  config,
  "Reports incorrectly named mapper files",
) {

  private val analyzer = MapperFileNamingAnalyzer()

  override fun visitKtFile(file: KtFile) {
    reportDiagnostics(listOfNotNull(analyzer.analyze(file)))
    super.visitKtFile(file)
  }
}
