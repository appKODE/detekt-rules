package ru.kode.detekt.rule.shared.analyzer

import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import ru.kode.detekt.rule.shared.KodeDiagnostic

/**
 * Checks that onEach's payload argument doesn't have a generic name
 */
class PayloadArgumentNameAnalyzer {
  /**
   * Checks `transitionTo { state, payload -> }` and `action { _, _, payload -> }` calls anywhere inside the trailing
   * lambda of an `onEach(...) { }` call. A nested `onEach` is checked again by the caller, as in 1.x.
   */
  fun analyze(expression: KtCallExpression): List<KodeDiagnostic> {
    if (expression.calleeExpression?.text != "onEach") return emptyList()
    val body = expression.lambdaArgumentAt(1)?.bodyExpression ?: return emptyList()
    val diagnostics = mutableListOf<KodeDiagnostic>()
    body.accept(
      object : KtTreeVisitorVoid() {
        override fun visitCallExpression(expression: KtCallExpression) {
          val payloadIndex = when (expression.calleeExpression?.text) {
            "transitionTo" -> 1
            "action" -> 2
            else -> null
          }
          val payload = payloadIndex?.let { expression.lambdaArgumentAt(0)?.valueParameters?.getOrNull(it) }
          if (payload?.name == "payload") {
            diagnostics += KodeDiagnostic(
              "Parameter name \"payload\" is too generic, use more specific name instead",
              payload,
            )
          }
          super.visitCallExpression(expression)
        }
      },
    )
    return diagnostics
  }

  private fun KtCallExpression.lambdaArgumentAt(index: Int) =
    (valueArguments.getOrNull(index) as? KtLambdaArgument)?.getLambdaExpression()
}
