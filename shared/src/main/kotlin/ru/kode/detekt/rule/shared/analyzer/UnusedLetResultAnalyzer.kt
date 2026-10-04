package ru.kode.detekt.rule.shared.analyzer

import org.jetbrains.kotlin.psi.KtAnnotatedExpression
import org.jetbrains.kotlin.psi.KtAnonymousInitializer
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCatchClause
import org.jetbrains.kotlin.psi.KtContainerNodeForControlStructureBody
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFinallySection
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtLabeledExpression
import org.jetbrains.kotlin.psi.KtLoopExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtParenthesizedExpression
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtTryExpression
import org.jetbrains.kotlin.psi.KtWhenEntry
import ru.kode.detekt.rule.shared.KodeDiagnostic

/**
 * Reports a `let { }` call used as a statement: `let` exists to transform its receiver, a call made for side effects
 * reads better as an `if` check or as `also`.
 *
 * Wrong:
 *
 * ```
 * fun show(error: Error?) {
 *   error?.let { showSnackbar(it.message) }
 * }
 * ```
 *
 * Correct:
 *
 * ```
 * fun show(error: Error?) {
 *   if (error != null) showSnackbar(error.message)
 * }
 * ```
 */
class UnusedLetResultAnalyzer {
  fun analyze(call: KtCallExpression): KodeDiagnostic? {
    if ((call.calleeExpression as? KtNameReferenceExpression)?.getReferencedName() != "let") return null
    if (call.lambdaArguments.isEmpty()) return null
    val qualified = call.parent as? KtQualifiedExpression ?: return null
    if (qualified.selectorExpression != call || !qualified.isStatement()) return null
    return KodeDiagnostic("Result of \"let\" is not used: use an \"if\" check or \"also\" instead", call)
  }

  /** True when the value of this expression is certainly discarded. */
  private fun KtExpression.isStatement(): Boolean = when (val parent = parent) {
    is KtBlockExpression -> parent.statements.last() != this || parent.isStatement()

    // The last statement of a lambda may be its result, and whether the lambda returns Unit is not known
    // without types, so it is never reported. Resolve the lambda's return type to cover `forEach { it?.let { } }`
    is KtFunctionLiteral -> false

    is KtDeclarationWithBody, is KtAnonymousInitializer, is KtFinallySection -> this is KtBlockExpression

    is KtTryExpression -> parent.isStatement()

    is KtCatchClause -> (parent.parent as KtExpression).isStatement()

    is KtContainerNodeForControlStructureBody -> when (val owner = parent.parent) {
      is KtLoopExpression -> true
      is KtIfExpression -> owner.isStatement()
      else -> false
    }

    is KtWhenEntry -> (parent.parent as KtExpression).isStatement()

    is KtParenthesizedExpression, is KtAnnotatedExpression, is KtLabeledExpression ->
      (parent as KtExpression).isStatement()

    else -> false
  }
}
