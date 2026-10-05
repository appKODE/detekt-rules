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
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
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
 * that an `open`, `abstract` or `sealed` class leaves visible to its subclasses. A parent job that is a property of the
 * class itself is no outside parent: it has to be cancelled in the class.
 */
class CoroutineScopeNotCancelledAnalyzer {
  // All of it goes by name, inside one class: a scope counts as cancelled by a `cancel` call whose receiver
  // starts with the scope property or with a name used to build the scope (`CoroutineScope(job)` + `job.cancel()`),
  // or that is made in a `with(scope) { }`, `scope.run { }` or `scope.apply { }` block, or on the parameter of a
  // `scope.let { }` or `scope.also { }` block. The blocks are told by the name of the function, and so are the
  // coroutine builders whose lambda has a scope of its own, so a `cancel()` in the lambda of another function with a
  // scope receiver still counts, and a parameter of `let` shadowed by a nested lambda is taken for the scope. A scope
  // cancelled by whoever it is handed to is reported; a scope created in `by lazy { }` or in `init` is not seen; a
  // scope that subclasses can see is trusted to be cancelled by one of them, since neither engine can list the
  // subclasses of a class that is not sealed
  fun analyze(klass: KtClass): List<KodeDiagnostic> {
    val inheritable = klass.hasModifier(KtTokens.OPEN_KEYWORD) || klass.hasModifier(KtTokens.ABSTRACT_KEYWORD) ||
      klass.hasModifier(KtTokens.SEALED_KEYWORD)
    val properties = klass.body?.properties.orEmpty()
    // a job with a parent (`val job = Job(parentJob)`) ends with that parent, like one that comes from outside
    val own = properties
      .filterNot { property -> property.initializer?.takeIf { it.calleeName() in jobFactories }.hasArguments() }
      .mapTo(mutableSetOf()) { it.name }
    val scopes = properties
      .filter { it.initializer?.calleeName() in scopeFactories }
      .filterNot { it.hasParent(own) || (inheritable && !it.hasModifier(KtTokens.PRIVATE_KEYWORD)) }
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
   * of the context, so the scope has no parent then, and neither has it when the parent job is one of the [own]
   * properties of the class: that one lives as long as the class does. A `coroutineContext` named in a lambda of the
   * initializer is not what the scope is built from.
   */
  private fun KtProperty.hasParent(own: Set<String?>): Boolean {
    val jobs = initializer?.collectDescendantsOfType<KtCallExpression> { it.calleeName() in jobFactories }.orEmpty()
    return jobs.any { job -> job.valueArguments.any { it.getArgumentExpression()?.rootName() !in own } } ||
      (
        jobs.isEmpty() &&
          initializer?.anyDescendantOfType<KtNameReferenceExpression>(
            canGoInside = { it !is KtLambdaExpression },
            predicate = { it.getReferencedName() == "coroutineContext" },
          ) == true
        )
  }

  /**
   * The names a `cancel` call cancels: the root of its receiver, or the receivers of the blocks it is made in, up to
   * a coroutine builder: in its lambda `cancel()` cancels the coroutine, not the scope around it.
   */
  private fun KtCallExpression.cancelledNames(): List<String> {
    val lambdas = parents.filterIsInstance<KtLambdaExpression>()
    val qualified = parent as? KtQualifiedExpression
    if (qualified?.selectorExpression == this) {
      val root = qualified.receiverExpression.rootName()
      return listOfNotNull(lambdas.firstNotNullOfOrNull { it.argumentName(root) } ?: root)
    }
    return lambdas.takeWhile { it.call()?.calleeName() !in scopeBuilders }.mapNotNull { it.receiverName() }.toList()
  }

  /** `scope` for the lambda of `with(scope) { }`, of `scope.run { }` and of `scope.apply { }`. */
  private fun KtLambdaExpression.receiverName(): String? {
    val call = call()
    return when (call?.calleeName()) {
      "with" -> call.valueArguments.firstOrNull()?.getArgumentExpression()?.rootName()
      "run", "apply" -> call.receiverName()
      else -> null
    }
  }

  /** `scope` for the lambda of `scope.let { }` and of `scope?.also { }` whose parameter, or `it`, is [parameter]. */
  private fun KtLambdaExpression.argumentName(parameter: String?): String? {
    val call = call()?.takeIf { it.calleeName() in argumentBlocks } ?: return null
    val name = if (valueParameters.isEmpty()) "it" else valueParameters.first().name
    return call.receiverName().takeIf { name == parameter }
  }

  /** The call this lambda is passed to as a trailing lambda. */
  private fun KtLambdaExpression.call(): KtCallExpression? = getStrictParentOfType<KtCallExpression>()
    ?.takeIf { call -> call.lambdaArguments.any { it.getLambdaExpression() == this } }

  private fun KtCallExpression.receiverName(): String? =
    (parent as? KtQualifiedExpression)?.takeIf { it.selectorExpression == this }?.receiverExpression?.rootName()

  private fun KtProperty.isBuiltFromAny(names: Set<String>): Boolean =
    initializer?.collectDescendantsOfType<KtNameReferenceExpression> { it.getReferencedName() in names }
      .orEmpty().isNotEmpty()

  private fun KtExpression?.hasArguments(): Boolean = (this as? KtCallExpression)?.valueArguments?.isNotEmpty() == true

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
    val argumentBlocks = setOf("let", "also")
    val scopeBuilders = setOf(
      "launch",
      "async",
      "coroutineScope",
      "supervisorScope",
      "withContext",
      "withTimeout",
      "withTimeoutOrNull",
      "runBlocking",
    )
  }
}
