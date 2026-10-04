package ru.kode.detekt.rule.shared.analyzer

import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.isPublic
import ru.kode.detekt.rule.shared.KodeDiagnostic
import ru.kode.detekt.rule.shared.KodeSemantic

/**
 * Reports a public `suspend` function of a reactive model: a model exposes its work as tasks, so that callers start
 * it and observe the results instead of suspending on it.
 *
 * Wrong:
 *
 * ```
 * class ProfileModel(scope: CoroutineScope) : ReactiveModel(scope) {
 *   suspend fun load(): Profile = repository.profile()
 * }
 * ```
 *
 * Correct:
 *
 * ```
 * class ProfileModel(scope: CoroutineScope) : ReactiveModel(scope) {
 *   val load = task<Unit, Profile> { repository.profile() }
 * }
 * ```
 */
class PublicSuspendFunctionInReactiveModelAnalyzer(
  private val baseClasses: List<String>,
  private val semantic: KodeSemantic,
) {
  fun analyze(klass: KtClass): List<KodeDiagnostic> {
    // the base class itself is not a model
    if (klass.name in baseClasses || semantic.superClassifierNames(klass).none { it in baseClasses }) {
      return emptyList()
    }
    return klass.body?.functions.orEmpty()
      .filter { it.hasModifier(KtTokens.SUSPEND_KEYWORD) && it.isPublic && !it.hasModifier(KtTokens.OVERRIDE_KEYWORD) }
      .map { function ->
        KodeDiagnostic(
          "Reactive model \"${klass.name}\" should not expose public suspend function \"${function.name}\": " +
            "make it a task or make it private",
          function,
        )
      }
  }
}
