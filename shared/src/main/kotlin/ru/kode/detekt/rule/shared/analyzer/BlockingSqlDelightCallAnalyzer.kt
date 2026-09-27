package ru.kode.detekt.rule.shared.analyzer

import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.parents
import ru.kode.detekt.rule.shared.KodeDiagnostic
import ru.kode.detekt.rule.shared.KodeSemantic

/**
 * Reports SqlDelight DB calls not made within the coroutine context: inside a `suspend` function, a call of a
 * member of `Transacter` (or of any of its subtypes: the generated database and queries) must be nested in a
 * `withContext(...)` call.
 *
 * The whole body of a suspend function is checked, non-suspend local functions included: they can only be called
 * from it, where a blocking call blocks too. A nested suspend function is checked on its own, and a `withContext`
 * enclosing a local function covers its calls (1.x reported those, and the calls of nested suspend functions twice).
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
        override fun visitNamedFunction(nested: KtNamedFunction) {
          if (nested == function || !nested.hasModifier(KtTokens.SUSPEND_KEYWORD)) super.visitNamedFunction(nested)
        }

        override fun visitCallExpression(expression: KtCallExpression) {
          super.visitCallExpression(expression)
          if (!semantic.isMemberCallDeclaredInSubtypeOf(expression, transacter)) return
          val inContext = expression.parents
            .takeWhile { it !is KtNamedFunction || it.isLocal }
            .any { it is KtCallExpression && it.calleeExpression?.text == "withContext" }
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
