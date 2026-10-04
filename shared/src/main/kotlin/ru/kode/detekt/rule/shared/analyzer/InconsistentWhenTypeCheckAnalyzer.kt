package ru.kode.detekt.rule.shared.analyzer

import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtWhenConditionIsPattern
import org.jetbrains.kotlin.psi.KtWhenConditionWithExpression
import org.jetbrains.kotlin.psi.KtWhenExpression
import ru.kode.detekt.rule.shared.KodeDiagnostic

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
 */
class InconsistentWhenTypeCheckAnalyzer {
  // An object is recognised by its PascalCase name; a PascalCase constant or enum entry in a `when` that
  // also has `is` branches is reported too. Resolve the reference to an object declaration if that gets noisy
  private val objectName = Regex("[A-Z][A-Za-z0-9]*[a-z][A-Za-z0-9]*")

  fun analyze(expression: KtWhenExpression): List<KodeDiagnostic> {
    if (expression.subjectExpression == null) return emptyList()
    val conditions = expression.entries.flatMap { it.conditions.asList() }
    if (conditions.none { it is KtWhenConditionIsPattern }) return emptyList()
    return conditions.filterIsInstance<KtWhenConditionWithExpression>()
      .filter { it.expression?.objectName()?.matches(objectName) == true }
      .map { KodeDiagnostic("Use \"is ${it.text}\" like the other branches of this \"when\" do", it) }
  }

  /** Last segment of a plain name or of a chain of plain names, null for anything else. */
  private fun KtExpression.objectName(): String? = when (this) {
    is KtNameReferenceExpression -> getReferencedName()
    is KtDotQualifiedExpression -> selectorExpression?.takeIf { receiverExpression.objectName() != null }?.objectName()
    else -> null
  }
}
