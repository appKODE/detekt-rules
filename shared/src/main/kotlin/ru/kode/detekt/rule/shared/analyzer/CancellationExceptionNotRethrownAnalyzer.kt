package ru.kode.detekt.rule.shared.analyzer

import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCatchClause
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.KtThrowExpression
import org.jetbrains.kotlin.psi.KtTryExpression
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import ru.kode.detekt.rule.shared.KodeDiagnostic
import ru.kode.detekt.rule.shared.KodeSemantic

/**
 * Reports a `catch` of a broad exception type, or of `CancellationException` itself, that does not rethrow, when its
 * `try` block calls a `suspend` function: it swallows `CancellationException`, so the coroutine keeps running after
 * it was cancelled.
 *
 * Wrong:
 *
 * ```
 * suspend fun load() {
 *   try {
 *     fetch()
 *   } catch (e: Exception) {
 *     log(e)
 *   }
 * }
 * ```
 *
 * Correct:
 *
 * ```
 * suspend fun load() {
 *   try {
 *     fetch()
 *   } catch (e: CancellationException) {
 *     throw e
 *   } catch (e: Exception) {
 *     log(e)
 *   }
 * }
 * ```
 *
 * Also correct: `throw e`, `coroutineContext.ensureActive()` or a call of one of [rethrowFunctions] in the broad
 * `catch` itself.
 *
 * The calls of the `try` block are resolved: a `try` in a suspend lambda (`scope.launch { try { } catch ... }`) is
 * checked like one in a `suspend` function, and a `try` around code that never suspends is not reported.
 */
class CancellationExceptionNotRethrownAnalyzer(
  private val semantic: KodeSemantic,
  private val rethrowFunctions: List<String>,
) {
  // The caught type and the rethrow are matched by name, so a `throw e` under a condition that excludes
  // cancellation, or in a lambda of the `catch` that is never run, counts as a rethrow. Resolve the caught type and
  // the thrown expression to cover those
  fun analyze(expression: KtTryExpression): KodeDiagnostic? {
    val clause = expression.catchClauses.firstOrNull { it.caughtType() in cancellationTypes } ?: return null
    // cancellation never reaches the clauses below this one
    if (clause.rethrows() || !expression.tryBlock.suspends()) return null
    return KodeDiagnostic(
      "Catching \"${clause.caughtType()}\" around a suspending call swallows CancellationException: " +
        "rethrow it or call \"ensureActive()\"",
      clause,
    )
  }

  /**
   * Whether the block calls a `suspend` function itself. Code that runs elsewhere than the block it is written in
   * does not count: a local function, an object, and a lambda or an anonymous function, except one that an inline
   * function runs in place.
   */
  // Only calls are suspension points here, so a `try` that suspends through an operator, an infix call, a
  // `for` over a channel or a delegated property alone is not reported, and neither is one that suspends in the
  // lambda of an infix call. Resolve those like detekt's SuspendFunSwallowedCancellation does if they turn out to
  // matter
  private fun KtBlockExpression.suspends(): Boolean = anyDescendantOfType<KtCallExpression>(
    canGoInside = { element ->
      when (element) {
        is KtClassOrObject -> false
        is KtLambdaExpression, is KtNamedFunction -> (element as KtExpression).isInlinedArgument()
        else -> true
      }
    },
    predicate = semantic::isSuspendCall,
  )

  private fun KtExpression.isInlinedArgument(): Boolean {
    // a label, parentheses or an annotation can stand between the argument and its lambda
    val argument = getStrictParentOfType<KtValueArgument>()
      ?.takeIf { KtPsiUtil.deparenthesize(it.getArgumentExpression()) == this }
      ?: return false
    val call = argument.getStrictParentOfType<KtCallExpression>()?.takeIf { argument in it.valueArguments }
    return call != null && semantic.isInlinedArgument(call, argument)
  }

  private fun KtCatchClause.caughtType(): String? =
    (catchParameter?.typeReference?.typeElement as? KtUserType)?.referencedName

  private fun KtCatchClause.rethrows(): Boolean {
    val body = catchBody ?: return false
    val parameter = catchParameter?.name
    return body.anyDescendantOfType<KtThrowExpression> { it.thrownExpression?.text == parameter } ||
      body.anyDescendantOfType<KtCallExpression> {
        val callee = it.calleeExpression?.text
        callee == "ensureActive" || callee in rethrowFunctions
      }
  }

  private companion object {
    val cancellationTypes =
      setOf("CancellationException", "Throwable", "Exception", "RuntimeException", "IllegalStateException")
  }
}
