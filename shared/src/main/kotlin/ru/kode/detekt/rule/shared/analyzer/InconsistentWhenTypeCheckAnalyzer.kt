package ru.kode.detekt.rule.shared.analyzer

import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.KtWhenConditionIsPattern
import org.jetbrains.kotlin.psi.KtWhenConditionWithExpression
import org.jetbrains.kotlin.psi.KtWhenExpression
import ru.kode.detekt.rule.shared.KodeDiagnostic
import ru.kode.detekt.rule.shared.KodeSemantic

/**
 * Reports a `when` over a type hierarchy that checks some branches with `is` and compares others to an object by
 * equality: use `is` in every branch.
 *
 * Wrong:
 *
 * ```
 * when (state) {
 *   is State.Content -> show(state.items)
 *   State.Loading -> showProgress()
 * }
 * ```
 *
 * Correct:
 *
 * ```
 * when (state) {
 *   is State.Content -> show(state.items)
 *   is State.Loading -> showProgress()
 * }
 * ```
 *
 * Only a reference that resolves to an `object` declaration is reported: a constant or an enum entry
 * (`Dp.Unspecified`, `Color.Red`) cannot be checked with `is`.
 */
class InconsistentWhenTypeCheckAnalyzer(
  private val semantic: KodeSemantic,
) {
  fun analyze(expression: KtWhenExpression): List<KodeDiagnostic> {
    if (expression.subjectExpression == null) return emptyList()
    val conditions = expression.entries.flatMap { it.conditions.asList() }
    if (conditions.none { it is KtWhenConditionIsPattern }) return emptyList()
    return conditions.filterIsInstance<KtWhenConditionWithExpression>().mapNotNull { condition ->
      // `(State.Loading) ->`
      KtPsiUtil.deparenthesize(condition.expression)
        ?.takeIf { it.lastName()?.let(semantic::isObjectReference) == true }
        ?.let { KodeDiagnostic("Use \"is ${it.text}\" like the other branches of this \"when\" do", condition) }
    }
  }

  /** Last segment of a plain name or of a chain of plain names, null for anything else. */
  private fun KtExpression.lastName(): KtNameReferenceExpression? = when (this) {
    is KtNameReferenceExpression -> this
    is KtDotQualifiedExpression -> selectorExpression?.takeIf { receiverExpression.lastName() != null }?.lastName()
    else -> null
  }
}
