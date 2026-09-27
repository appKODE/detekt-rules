package ru.kode.detekt.rule

import dev.detekt.api.Config
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import ru.kode.detekt.rule.shared.analyzer.ComponentFunctionCallAnalyzer

class ComponentFunctionCall(config: Config = Config.empty) : Rule(config, "Reports usage of \"comonentN\" functions") {

  private val analyzer = ComponentFunctionCallAnalyzer()

  override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
    val diagnostic = analyzer.analyze(expression)
    // 1.x did not descend into a reported chain
    if (diagnostic != null) reportDiagnostics(listOf(diagnostic)) else super.visitDotQualifiedExpression(expression)
  }
}
