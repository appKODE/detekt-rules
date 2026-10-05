package ru.kode.detekt.rule.shared.analyzer

import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtSuperTypeCallEntry
import ru.kode.detekt.rule.shared.KodeDiagnostic
import ru.kode.detekt.rule.shared.KodeSemantic

/**
 * Reports a sealed class or interface whose inheritors are all stateless objects: an enum class says the same with
 * less code and gives `entries` for free.
 *
 * Wrong:
 *
 * ```
 * sealed interface Direction {
 *   object Up : Direction
 *   object Down : Direction
 * }
 * ```
 *
 * Correct:
 *
 * ```
 * enum class Direction { Up, Down }
 * ```
 *
 * The inheritors are resolved, so the ones declared in other files count too.
 */
class SealedClassCouldBeEnumAnalyzer(private val semantic: KodeSemantic) {
  fun analyze(klass: KtClass): KodeDiagnostic? {
    if (!klass.hasModifier(KtTokens.SEALED_KEYWORD) || klass.typeParameters.isNotEmpty()) return null
    // an enum class cannot extend a class
    // A superclass named only by the `super()` calls of secondary constructors is not seen. Compare the
    // resolved supertypes of the class if such sealed classes show up
    if (klass.superTypeListEntries.any { it is KtSuperTypeCallEntry }) return null
    val inheritors = semantic.sealedInheritors(klass)
    if (inheritors.size < 2 || !inheritors.all { it.couldBeEnumEntry() }) return null
    return KodeDiagnostic("Sealed type \"${klass.name}\" has only object inheritors: use an enum class instead", klass)
  }

  private fun KtClassOrObject.couldBeEnumEntry(): Boolean = this is KtObjectDeclaration &&
    !isCompanion() &&
    !isObjectLiteral() &&
    superTypeListEntries.size == 1 &&
    declarations.none { it is KtProperty && !it.hasModifier(KtTokens.OVERRIDE_KEYWORD) }
}
