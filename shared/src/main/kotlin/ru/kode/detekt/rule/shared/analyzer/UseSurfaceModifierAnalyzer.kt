package ru.kode.detekt.rule.shared.analyzer

import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression
import ru.kode.detekt.rule.shared.KodeDiagnostic
import ru.kode.detekt.rule.shared.hasAnnotation

/**
 * Suggests replacing shaped background used with clip/shadow/clickable with the \"surface\" modifier
 *
 * Wrong:
 *
 * ```
 * Box(
 *   modifier = Modifier
 *     .clickable(onClick = {})
 *     .background(shape = RoundCornerShape(12.dp))
 *     .clip(RoundCornerShape(12.dp))
 * )
 * ```
 *
 * Correct:
 *
 * ```
 * Box(
 *   modifier = Modifier
 *     .surface(shape = RoundCornerShape(12.dp), onClick = {})
 * )
 * ```
 */
class UseSurfaceModifierAnalyzer {
  /**
   * Checks the whole body of a `@Composable` function, nested functions included. As in 1.x, the caller must not
   * visit nested functions separately, and only the outermost `Modifier`/`modifier` chain of an expression is
   * checked.
   */
  fun analyze(function: KtNamedFunction): List<KodeDiagnostic> {
    if (!function.hasAnnotation("Composable")) return emptyList()
    val diagnostics = mutableListOf<KodeDiagnostic>()
    function.bodyExpression?.accept(
      object : KtTreeVisitorVoid() {
        override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
          if (!expression.text.startsWith("Modifier") && !expression.text.startsWith("modifier")) {
            super.visitDotQualifiedExpression(expression)
            return
          }
          val modifiers = expression.splitToExpressions().drop(1) // drop "Modifier." or "modifier."
          val combined = modifiers.any { it.isCallTo("clickable") || it.isCallTo("clip") || it.isCallTo("shadow") }
          if (modifiers.any { it.isBackgroundWithShape() } && combined) {
            diagnostics += KodeDiagnostic(
              "Use \"Modifier.surface()\" instead of combining shaped background with clickable/clip/shadow",
              expression,
            )
          }
        }
      },
    )
    return diagnostics
  }

  private fun KtExpression.isCallTo(name: String): Boolean =
    this is KtCallExpression && getCallNameExpression()?.text == name

  private fun KtExpression.isBackgroundWithShape(): Boolean =
    isCallTo("background") && (this as KtCallExpression).valueArguments.any { it.getArgumentName()?.text == "shape" }

  // Splits Modifier.background().shape().color() to "Modifier", "background()", "shape()", "color()"
  private fun KtDotQualifiedExpression.splitToExpressions(): List<KtExpression> {
    val expressions = ArrayDeque<KtExpression>()
    var e: KtExpression? = this
    while (e != null) {
      val selector = (e as? KtDotQualifiedExpression)?.selectorExpression
      if (selector != null) {
        expressions.addFirst(selector)
        e = (e as KtDotQualifiedExpression).receiverExpression
      } else {
        expressions.addFirst(e)
        e = null
      }
    }
    return expressions
  }
}
