package ru.kode.detekt.rule.shared.analyzer

import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCatchClause
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtThrowExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.KtTryExpression
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import ru.kode.detekt.rule.shared.KodeDiagnostic

/**
 * Reports a `catch` of a broad exception type in a `suspend` function that does not rethrow: it also catches
 * `CancellationException`, so the coroutine keeps running after it was cancelled.
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
 */
class CancellationExceptionNotRethrownAnalyzer(
  private val rethrowFunctions: List<String>,
) {
  // Only `try` in the body of a `suspend fun` is checked, and the caught type and the rethrow are matched
  // by name. A `try` in a suspend lambda of a regular function (`scope.launch { try { } catch ... }`) is not seen,
  // and a `throw e` under a condition that excludes cancellation counts as a rethrow. Resolve the lambda's type and
  // the thrown expression to cover those
  fun analyze(function: KtNamedFunction): List<KodeDiagnostic> {
    if (!function.hasModifier(KtTokens.SUSPEND_KEYWORD)) return emptyList()
    val diagnostics = mutableListOf<KodeDiagnostic>()
    function.accept(
      object : KtTreeVisitorVoid() {
        override fun visitNamedFunction(nested: KtNamedFunction) {
          if (nested == function || !nested.hasModifier(KtTokens.SUSPEND_KEYWORD)) super.visitNamedFunction(nested)
        }

        override fun visitTryExpression(expression: KtTryExpression) {
          super.visitTryExpression(expression)
          diagnostics += listOfNotNull(analyze(expression))
        }
      },
    )
    return diagnostics
  }

  private fun analyze(expression: KtTryExpression): KodeDiagnostic? {
    for (clause in expression.catchClauses) {
      val type = (clause.catchParameter?.typeReference?.typeElement as? KtUserType)?.referencedName
      val rethrows = clause.rethrows()
      // cancellation never reaches the clauses below this one
      if (type == "CancellationException" && rethrows) return null
      if (type in broadTypes && !rethrows) {
        return KodeDiagnostic(
          "Catching \"$type\" in a suspend function swallows CancellationException: " +
            "rethrow it or call \"ensureActive()\"",
          clause,
        )
      }
    }
    return null
  }

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
    val broadTypes = setOf("Throwable", "Exception", "RuntimeException", "IllegalStateException")
  }
}
