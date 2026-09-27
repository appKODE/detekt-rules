package ru.kode.detekt.rule.shared.analyzer

import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import ru.kode.detekt.rule.shared.KodeDiagnostic
import ru.kode.detekt.rule.shared.ownSubExpressions

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

  /** Checks only the calls this link of a chain owns: the caller visits every link, lambdas included. */
  fun analyze(expression: KtDotQualifiedExpression): List<KodeDiagnostic> =
    expression.ownSubExpressions().filterIsInstance<KtCallExpression>()
      .filter { call ->
        call.valueArguments.isEmpty() && call.calleeExpression?.text?.let(componentFunctionNameRegex::matches) == true
      }
      .map {
        KodeDiagnostic(
          "Instead of calling \"componentN\" functions, use property access or any other means available",
          it,
        )
      }
}
