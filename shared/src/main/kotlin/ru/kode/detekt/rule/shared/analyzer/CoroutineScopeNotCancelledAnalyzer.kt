package ru.kode.detekt.rule.shared.analyzer

import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtParenthesizedExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtThisExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import ru.kode.detekt.rule.shared.KodeDiagnostic

/**
 * Reports a class that creates a coroutine scope and never cancels it: its coroutines outlive the class.
 *
 * Wrong:
 *
 * ```
 * class Player {
 *   private val scope = CoroutineScope(Dispatchers.Main)
 * }
 * ```
 *
 * Correct:
 *
 * ```
 * class Player {
 *   private val scope = CoroutineScope(Dispatchers.Main)
 *
 *   fun release() {
 *     scope.cancel()
 *   }
 * }
 * ```
 */
class CoroutineScopeNotCancelledAnalyzer {
  // A scope counts as cancelled only by a `cancel` call in the same class whose receiver starts with the
  // scope property, or with a name used to build the scope (`CoroutineScope(job)` + `job.cancel()`). A scope
  // cancelled through `with(scope) { cancel() }`, by a subclass, by whoever it is handed to or by the parent job it
  // was built from (`CoroutineScope(parent.coroutineContext)`) is reported; a scope created in `by lazy { }` or in
  // `init` is not seen. Follow the references with type resolution to be exact
  fun analyze(klass: KtClass): List<KodeDiagnostic> {
    val scopes = klass.body?.properties.orEmpty().filter { it.initializer?.calleeName() in scopeFactories }
    if (scopes.isEmpty()) return emptyList()
    // a nested class cancels its own scopes
    val cancelled = klass
      .collectDescendantsOfType<KtCallExpression> {
        it.calleeName() == "cancel" && it.getStrictParentOfType<KtClass>() == klass
      }
      .mapNotNull { call -> (call.parent as? KtQualifiedExpression)?.receiverExpression?.rootName() }
      .toSet()
    return scopes.filterNot { it.name in cancelled || it.isBuiltFromAny(cancelled) }.map { scope ->
      KodeDiagnostic(
        "CoroutineScope \"${scope.name}\" is created in class \"${klass.name}\" but never cancelled",
        scope,
      )
    }
  }

  private fun KtProperty.isBuiltFromAny(names: Set<String>): Boolean =
    initializer?.collectDescendantsOfType<KtNameReferenceExpression> { it.getReferencedName() in names }
      .orEmpty().isNotEmpty()

  private fun KtExpression.calleeName(): String? =
    ((this as? KtCallExpression)?.calleeExpression as? KtNameReferenceExpression)?.getReferencedName()

  /**
   * First name of a receiver chain: `scope` for `scope.coroutineContext`, for `this.scope` and for
   * `scope.coroutineContext[Job]`. A chain with a call in it (`scope.launch { }`) is the result of that call, not
   * the scope.
   */
  private fun KtExpression.rootName(): String? = when (this) {
    is KtNameReferenceExpression -> getReferencedName()

    is KtQualifiedExpression -> when {
      receiverExpression is KtThisExpression -> selectorExpression?.rootName()
      selectorExpression is KtCallExpression -> null
      else -> receiverExpression.rootName()
    }

    is KtArrayAccessExpression -> arrayExpression?.rootName()

    is KtParenthesizedExpression -> expression?.rootName()

    else -> null
  }

  private companion object {
    val scopeFactories = setOf("CoroutineScope", "MainScope")
  }
}
