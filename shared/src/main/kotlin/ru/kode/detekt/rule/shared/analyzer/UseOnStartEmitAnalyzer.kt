package ru.kode.detekt.rule.shared.analyzer

import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtLambdaArgument
import ru.kode.detekt.rule.shared.KodeDiagnostic
import ru.kode.detekt.rule.shared.forEachSubExpression

/**
 * Suggests replacing onStart { emit(value) } with onStartEmit(value)
 *
 * Wrong:
 *
 * ```
 * myFlow.onStart { emit(value) }
 * ```
 *
 * Correct:
 *
 * ```
 * myFlow.onStartEmit(value)
 * ```
 */
class UseOnStartEmitAnalyzer {
  /**
   * Returns the first offending `onStart` call of the chain, outermost first. As in 1.x, the caller must not visit
   * the children of a reported chain, so a second offending call in the same chain is not reported.
   */
  fun analyze(expression: KtDotQualifiedExpression): KodeDiagnostic? {
    expression.forEachSubExpression { e ->
      if (e is KtCallExpression && e.calleeExpression?.text == "onStart") {
        val lambda = (e.valueArguments.getOrNull(0) as? KtLambdaArgument)?.getLambdaExpression()
        val emits = lambda?.bodyExpression?.statements.orEmpty()
          .any { it is KtCallExpression && it.calleeExpression?.text == "emit" }
        if (emits) return KodeDiagnostic("Use Flow.onStartEmit() instead of Flow.onStart()", e)
      }
    }
    return null
  }
}
