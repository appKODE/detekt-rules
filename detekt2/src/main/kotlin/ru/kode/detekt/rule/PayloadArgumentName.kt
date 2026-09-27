package ru.kode.detekt.rule

import dev.detekt.api.Config
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtCallExpression
import ru.kode.detekt.rule.shared.analyzer.PayloadArgumentNameAnalyzer

class PayloadArgumentName(config: Config = Config.empty) : Rule(config, "Checks onEach payload argument name") {

  private val analyzer = PayloadArgumentNameAnalyzer()

  override fun visitCallExpression(expression: KtCallExpression) {
    reportDiagnostics(analyzer.analyze(expression))
    super.visitCallExpression(expression)
  }
}
