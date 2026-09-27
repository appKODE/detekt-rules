package ru.kode.detekt.rule

import dev.detekt.api.Config
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtFile
import ru.kode.detekt.rule.shared.analyzer.MapperFileNamingAnalyzer

class MapperFileNaming(config: Config = Config.empty) : Rule(config, "Reports incorrectly named mapper files") {

  private val analyzer = MapperFileNamingAnalyzer()

  override fun visitKtFile(file: KtFile) {
    reportDiagnostics(listOfNotNull(analyzer.analyze(file)))
    super.visitKtFile(file)
  }
}
