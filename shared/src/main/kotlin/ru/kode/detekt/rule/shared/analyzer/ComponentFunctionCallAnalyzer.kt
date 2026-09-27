package ru.kode.detekt.rule.shared.analyzer

import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import ru.kode.detekt.rule.shared.KodeDiagnostic
import ru.kode.detekt.rule.shared.forEachSubExpression

/**
 * Suggests avoiding calling "componentN" functions directly
 *
 * Wrong:
 *
 * ```
 * data class Person(val name: String, val age: Int)
 * val person = Person("Dave", 21)
 *
 * val name = person.component1()
 * val age = person.component2()
 * ```
 *
 * Correct:
 *
 * ```
 * data class Person(val name: String, val age: Int)
 * val person = Person("Dave", 21)
 *
 * val name = person.name
 * val age = person.age
 * ```
 */
class ComponentFunctionCallAnalyzer {
  private val componentFunctionNameRegex = Regex("^component[1-9]\\d*$")

  /**
   * Returns the first `componentN()` call of the chain, outermost first. As in 1.x, the caller must not visit the
   * children of a reported chain, so further calls in the same chain (or in its lambdas) are not reported.
   */
  fun analyze(expression: KtDotQualifiedExpression): KodeDiagnostic? {
    expression.forEachSubExpression { e ->
      if (e is KtCallExpression) {
        val name = e.calleeExpression?.text
        if (name != null && e.valueArguments.isEmpty() && componentFunctionNameRegex.matches(name)) {
          return KodeDiagnostic(
            "Instead of calling \"componentN\" functions, use property access or any other means available",
            e,
          )
        }
      }
    }
    return null
  }
}
