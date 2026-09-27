package ru.kode.detekt.rule.shared.analyzer

import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import ru.kode.detekt.rule.shared.KodeDiagnostic
import ru.kode.detekt.rule.shared.KodeSemantic
import ru.kode.detekt.rule.shared.parentsOfTypeUntil

/**
 * Reports SqlDelight DB calls not made within the coroutine context: inside a `suspend` function, a call of a
 * member of `Transacter` (or of any of its subtypes: the generated database and queries) must be nested in a
 * `withContext(...)` call.
 *
 * The whole body of a suspend function is checked, nested functions included; a nested suspend function is then
 * checked again on its own (as in 1.x, so its calls are reported twice).
 */
class BlockingSqlDelightCallAnalyzer(
  sqlDelightPackage: String,
  private val semantic: KodeSemantic,
) {
  private val transacter = "$sqlDelightPackage.Transacter"

  fun analyze(function: KtNamedFunction): List<KodeDiagnostic> {
    if (!function.hasModifier(KtTokens.SUSPEND_KEYWORD)) return emptyList()
    val diagnostics = mutableListOf<KodeDiagnostic>()
    function.accept(
      object : KtTreeVisitorVoid() {
        override fun visitCallExpression(expression: KtCallExpression) {
          super.visitCallExpression(expression)
          if (!semantic.isMemberCallOnSubtypeOf(expression, transacter)) return
          val inContext = expression.parentsOfTypeUntil<KtCallExpression, KtNamedFunction>()
            .any { it.calleeExpression?.text == "withContext" }
          if (!inContext) {
            diagnostics += KodeDiagnostic(
              "Database queries/transactions should be wrapped in \"withContext(Dispatchers.IO)\" to prevent blocking",
              expression,
            )
          }
        }
      },
    )
    return diagnostics
  }
}
