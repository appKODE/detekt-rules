package ru.kode.detekt.rule

import dev.detekt.api.Config
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtNamedFunction
import ru.kode.detekt.rule.shared.analyzer.UseSurfaceModifierAnalyzer

class UseSurfaceModifier(config: Config = Config.empty) : Rule(
  config,
  "Suggests replacing shaped background used with clip/shadow/clickable with the \"surface\" modifier",
) {

  private val analyzer = UseSurfaceModifierAnalyzer()

  override fun visitNamedFunction(function: KtNamedFunction) {
    // no super call: the analyzer covers nested functions of a composable, others are skipped, like 1.x did
    reportDiagnostics(analyzer.analyze(function))
  }
}
