package ru.kode.detekt.rule.shared.analyzer

import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
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

  /**
   * Mirrors the 1.x visitor flag: it was set when entering a class (to "name contains Wiring") and cleared when
   * leaving *any* class. So the function's innermost class must be a Wiring class, and no other class inside it may
   * have been left before the check ran: none declared earlier in that class, and none local to the function (1.x
   * checked a function after visiting its body).
   */
  private fun KtNamedFunction.isInsideWiringClass(): Boolean {
    val owner = getStrictParentOfType<KtClass>() ?: return false
    if (owner.name?.contains("Wiring") != true) return false
    return owner.collectDescendantsOfType<KtClass> { it != owner }.none { it.startOffset < endOffset }
  }
}
