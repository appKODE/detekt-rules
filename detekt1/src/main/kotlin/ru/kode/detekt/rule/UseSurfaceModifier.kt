package ru.kode.detekt.rule

import io.gitlab.arturbosch.detekt.api.Config
import org.jetbrains.kotlin.psi.KtNamedFunction
import ru.kode.detekt.rule.shared.analyzer.UseSurfaceModifierAnalyzer

class UseSurfaceModifier(config: Config = Config.empty) : Detekt1SharedRule(
  config,
  "Suggests replacing shaped background used with clip/shadow/clickable with the \"surface\" modifier",
) {

  private val analyzer = UseSurfaceModifierAnalyzer()

  override fun visitNamedFunction(function: KtNamedFunction) {
    reportDiagnostics(analyzer.analyze(function))
    super.visitNamedFunction(function)
  }
}
