package ru.kode.detekt.rule

import dev.detekt.api.Config
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtClass
import ru.kode.detekt.rule.shared.analyzer.CoroutineScopeNotCancelledAnalyzer

class CoroutineScopeNotCancelled(config: Config = Config.empty) : Rule(
  config,
  "Reports coroutine scopes created in a class and never cancelled",
) {

  private val analyzer = CoroutineScopeNotCancelledAnalyzer()

  override fun visitClass(klass: KtClass) {
    reportDiagnostics(analyzer.analyze(klass))
    super.visitClass(klass)
  }
}
