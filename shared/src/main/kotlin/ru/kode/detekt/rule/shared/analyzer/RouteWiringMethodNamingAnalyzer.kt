package ru.kode.detekt.rule.shared.analyzer

import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import ru.kode.detekt.rule.shared.KodeDiagnostic

/**
 * Use "navigateOn" prefix when naming routing methods in wiring classes:
 *
 * ```
 * class UserDetailsWiring {
 *   fun navigateOnUpdateDetails() {
 *     coordinator.handleEvent(Event.UpdateDetailsRequested)
 *   }
 * }
 * ```
 */
class RouteWiringMethodNamingAnalyzer {
  private val handleCallRegex = Regex("[cC]oordinator\\.handleEvent")

  fun analyze(function: KtNamedFunction): KodeDiagnostic? {
    if (!function.isInsideWiringClass()) return null
    if (function.bodyExpression?.text?.contains(handleCallRegex) != true) return null
    if (function.name?.startsWith("navigateOn") != false) return null
    return KodeDiagnostic(
      "Use \"navigateOn\" prefix for routing-related function names inside a Wiring class",
      function,
    )
  }

  /** The function's innermost class (or enum entry) must be a Wiring class. */
  private fun KtNamedFunction.isInsideWiringClass() = getStrictParentOfType<KtClass>()?.name?.contains("Wiring") == true
}
