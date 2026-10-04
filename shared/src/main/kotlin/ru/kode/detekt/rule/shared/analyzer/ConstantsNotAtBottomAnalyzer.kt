package ru.kode.detekt.rule.shared.analyzer

import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtProperty
import ru.kode.detekt.rule.shared.KodeDiagnostic

/**
 * Reports constants declared above the code that uses them: top-level `const val`s go to the bottom of the file,
 * and a companion object with constants is the last member of its class.
 *
 * Wrong:
 *
 * ```
 * private const val PAGE_SIZE = 20
 *
 * class Pager {
 *   companion object {
 *     const val FIRST_PAGE = 0
 *   }
 *
 *   fun load() = Unit
 * }
 * ```
 *
 * Correct:
 *
 * ```
 * class Pager {
 *   fun load() = Unit
 *
 *   companion object {
 *     const val FIRST_PAGE = 0
 *   }
 * }
 *
 * private const val PAGE_SIZE = 20
 * ```
 *
 * Only properties may follow a top-level constant, so a file of constants alone is fine. One finding is reported
 * per file and per class, at the first misplaced declaration.
 */
class ConstantsNotAtBottomAnalyzer {
  fun analyze(file: KtFile): KodeDiagnostic? {
    val declarations = file.declarations
    val lastNonProperty = declarations.indexOfLast { it !is KtProperty }
    val constant = declarations.take(lastNonProperty.coerceAtLeast(0)).firstOrNull { it.isConstant() } ?: return null
    return KodeDiagnostic("Constant \"${constant.name}\" should be declared at the bottom of the file", constant)
  }

  fun analyze(klass: KtClass): KodeDiagnostic? {
    val companion = klass.companionObjects.firstOrNull { it.declarations.any { member -> member.isConstant() } }
    if (companion == null || klass.declarations.last() == companion) return null
    return KodeDiagnostic(
      "Companion object with constants should be the last declaration of class \"${klass.name}\"",
      companion,
    )
  }

  private fun KtDeclaration.isConstant(): Boolean = this is KtProperty && hasModifier(KtTokens.CONST_KEYWORD)
}
