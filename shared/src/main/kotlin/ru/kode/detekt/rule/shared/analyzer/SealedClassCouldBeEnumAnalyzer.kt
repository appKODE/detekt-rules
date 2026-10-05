package ru.kode.detekt.rule.shared.analyzer

import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtSuperTypeCallEntry
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import ru.kode.detekt.rule.shared.KodeDiagnostic

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
 */
class SealedClassCouldBeEnumAnalyzer {
  // Inheritors are searched in the same file and matched by the simple name of the supertype. A sealed type
  // with a non-object inheritor in another file is reported, a same-named type from another package can hide a
  // finding, and two same-named sealed types of one file (`First.State`, `Second.State`) share their inheritors, so
  // neither is reported once one of them has a class inheritor. A sealed class that extends a class through `super()`
  // calls of secondary constructors only is not excluded either. Needs a type-resolved inheritor search to be exact
  fun analyze(klass: KtClass): KodeDiagnostic? {
    if (!klass.hasModifier(KtTokens.SEALED_KEYWORD) || klass.typeParameters.isNotEmpty()) return null
    // an enum class cannot extend a class
    if (klass.superTypeListEntries.any { it is KtSuperTypeCallEntry }) return null
    val inheritors = klass.containingKtFile.collectDescendantsOfType<KtClassOrObject> { it.extends(klass) }
    if (inheritors.size < 2 || !inheritors.all { it.couldBeEnumEntry() }) return null
    return KodeDiagnostic("Sealed type \"${klass.name}\" has only object inheritors: use an enum class instead", klass)
  }

  private fun KtClassOrObject.extends(klass: KtClass): Boolean =
    superTypeListEntries.any { (it.typeReference?.typeElement as? KtUserType)?.referencedName == klass.name }

  private fun KtClassOrObject.couldBeEnumEntry(): Boolean = this is KtObjectDeclaration &&
    !isCompanion() &&
    !isObjectLiteral() &&
    superTypeListEntries.size == 1 &&
    declarations.none { it is KtProperty && !it.hasModifier(KtTokens.OVERRIDE_KEYWORD) }
}
