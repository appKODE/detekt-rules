package ru.kode.detekt.rule.shared.analyzer

import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtLambdaArgument
import ru.kode.detekt.rule.shared.KodeDiagnostic
import ru.kode.detekt.rule.shared.ownSubExpressions

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
  /** Checks only the calls this link of a chain owns: the caller visits every link, lambdas included. */
  fun analyze(expression: KtDotQualifiedExpression): List<KodeDiagnostic> =
    expression.ownSubExpressions().filterIsInstance<KtCallExpression>()
      .filter { it.calleeExpression?.text == "onStart" && it.emitsInLambda() }
      .map { KodeDiagnostic("Use Flow.onStartEmit() instead of Flow.onStart()", it) }

  private fun KtCallExpression.emitsInLambda(): Boolean {
    val lambda = (valueArguments.getOrNull(0) as? KtLambdaArgument)?.getLambdaExpression()
    return lambda?.bodyExpression?.statements.orEmpty()
      .any { it is KtCallExpression && it.calleeExpression?.text == "emit" }
  }
}
