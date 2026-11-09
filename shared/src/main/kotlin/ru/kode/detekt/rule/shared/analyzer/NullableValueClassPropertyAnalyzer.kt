package ru.kode.detekt.rule.shared.analyzer

import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNullableType
import ru.kode.detekt.rule.shared.KodeDiagnostic

/**
 * Reports value classes wrapping a nullable type: such a value has two "empty" states (`null` and a wrapper around
 * `null`) and is boxed wherever it is used as nullable itself.
 *
 * Wrong:
 *
 * ```
 * @JvmInline
 * value class UserId(val value: String?)
 * ```
 *
 * Correct:
 *
 * ```
 * @JvmInline
 * value class UserId(val value: String)
 *
 * val id: UserId? = null
 * ```
 */
class NullableValueClassPropertyAnalyzer {
  fun analyze(klass: KtClass): KodeDiagnostic? {
    if (!klass.hasModifier(KtTokens.VALUE_KEYWORD)) return null
    val parameter = klass.primaryConstructorParameters.singleOrNull() ?: return null
    if (parameter.typeReference?.typeElement !is KtNullableType) return null
    return KodeDiagnostic(
      "Value class \"${klass.name}\" wraps a nullable type: make the property non-null and use \"${klass.name}?\"",
      parameter,
    )
  }
}
