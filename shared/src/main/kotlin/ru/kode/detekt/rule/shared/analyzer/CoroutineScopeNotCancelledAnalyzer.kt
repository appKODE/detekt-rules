package ru.kode.detekt.rule.shared.analyzer

import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtParenthesizedExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtThisExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.parents
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
 *
 * A scope whose lifetime is not the business of the class alone is not reported: one built from a parent
 * (`CoroutineScope(parent.coroutineContext)`, `CoroutineScope(Job(parentJob))`), which is cancelled with it, and one
 * that an `open`, `abstract` or `sealed` class leaves visible to its subclasses.
 */
class CoroutineScopeNotCancelledAnalyzer {
  // All of it goes by name, inside one class: a scope counts as cancelled by a `cancel` call whose receiver
  // starts with the scope property or with a name used to build the scope (`CoroutineScope(job)` + `job.cancel()`),
  // or that is made in a `with(scope) { }`, `scope.run { }` or `scope.apply { }` block. A scope cancelled by whoever
  // it is handed to is reported; a scope created in `by lazy { }` or in `init` is not seen; a scope that subclasses
  // can see is trusted to be cancelled by one of them, since neither engine can list the subclasses of a class that
  // is not sealed
  fun analyze(klass: KtClass): List<KodeDiagnostic> {
    val inheritable = klass.hasModifier(KtTokens.OPEN_KEYWORD) || klass.hasModifier(KtTokens.ABSTRACT_KEYWORD) ||
      klass.hasModifier(KtTokens.SEALED_KEYWORD)
    val scopes = klass.body?.properties.orEmpty()
      .filter { it.initializer?.calleeName() in scopeFactories }
      .filterNot { it.hasParent() || (inheritable && !it.hasModifier(KtTokens.PRIVATE_KEYWORD)) }
    if (scopes.isEmpty()) return emptyList()
    // a nested class cancels its own scopes
    val cancelled = klass
      .collectDescendantsOfType<KtCallExpression> {
        it.calleeName() == "cancel" && it.getStrictParentOfType<KtClass>() == klass
      }
      .flatMap { it.cancelledNames() }
      .toSet()
    return scopes.filterNot { it.name in cancelled || it.isBuiltFromAny(cancelled) }.map { scope ->
      KodeDiagnostic(
        "CoroutineScope \"${scope.name}\" is created in class \"${klass.name}\" but never cancelled",
        scope,
      )
    }
  }

  /**
   * Whether the scope is built from the context of another one (`CoroutineScope(parent.coroutineContext)`) or from a
   * job with a parent (`CoroutineScope(SupervisorJob(parentJob))`). A job of its own (`... + Job()`) replaces the one
   * of the context, so the scope has no parent then.
   */
  private fun KtProperty.hasParent(): Boolean {
    val jobs = initializer?.collectDescendantsOfType<KtCallExpression> { it.calleeName() in jobFactories }.orEmpty()
    return jobs.any { it.valueArguments.isNotEmpty() } ||
      (jobs.isEmpty() && isBuiltFromAny(setOf("coroutineContext")))
  }

  /** The names a `cancel` call cancels: the root of its receiver, or the receivers of the blocks it is made in. */
  private fun KtCallExpression.cancelledNames(): List<String> {
    val qualified = parent as? KtQualifiedExpression
    if (qualified?.selectorExpression == this) return listOfNotNull(qualified.receiverExpression.rootName())
    return parents.filterIsInstance<KtLambdaExpression>().mapNotNull { it.receiverName() }.toList()
  }

  /** `scope` for the lambda of `with(scope) { }`, of `scope.run { }` and of `scope.apply { }`. */
  private fun KtLambdaExpression.receiverName(): String? {
    val call = getStrictParentOfType<KtCallExpression>()?.takeIf { call ->
      call.lambdaArguments.any {
        it.getLambdaExpression() ==
          this
      }
    }
    return when (call?.calleeName()) {
      "with" -> call.valueArguments.firstOrNull()?.getArgumentExpression()?.rootName()
      "run", "apply" -> (call.parent as? KtQualifiedExpression)?.receiverExpression?.rootName()
      else -> null
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
    val jobFactories = setOf("Job", "SupervisorJob")
  }
}
