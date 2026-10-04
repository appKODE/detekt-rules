package ru.kode.detekt.rule

import io.gitlab.arturbosch.detekt.api.Config
import org.jetbrains.kotlin.psi.KtClass
import ru.kode.detekt.rule.shared.analyzer.CoroutineScopeNotCancelledAnalyzer

class CoroutineScopeNotCancelled(config: Config = Config.empty) : Detekt1SharedRule(
  config,
  "Reports coroutine scopes created in a class and never cancelled",
) {

  private val analyzer = CoroutineScopeNotCancelledAnalyzer()

  override fun visitClass(klass: KtClass) {
    reportDiagnostics(analyzer.analyze(klass))
    super.visitClass(klass)
  }
}
