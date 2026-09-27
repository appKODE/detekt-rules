package ru.kode.detekt.rule

import io.gitlab.arturbosch.detekt.api.Config
import org.jetbrains.kotlin.psi.KtNamedFunction
import ru.kode.detekt.rule.shared.analyzer.RouteWiringMethodNamingAnalyzer

class RouteWiringMethodNaming(config: Config = Config.empty) : Detekt1SharedRule(
  config,
  "Reports incorrect naming of routing methods inside wiring classes",
) {

  private val analyzer = RouteWiringMethodNamingAnalyzer()

  override fun visitNamedFunction(function: KtNamedFunction) {
    super.visitNamedFunction(function)
    reportDiagnostics(listOfNotNull(analyzer.analyze(function)))
  }
}
