package ru.kode.detekt.rule.shared.analyzer

import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtUserType
import ru.kode.detekt.rule.shared.KodeDiagnostic
import ru.kode.detekt.rule.shared.hasAnnotation

/**
 * Reports data classes declared in a `ui` package (any package segment) without the `@Immutable` annotation.
 * Descendants of the classes listed in [ignoreDescendantsOf] are skipped. Without type resolution only the direct
 * supertypes written in the declaration count, matched by their short or fully qualified name; the latter is taken
 * from the written name, an explicit import (aliases included) or, failing that, the file's package.
 */
class ImmutableDataClassAnalyzer(
  private val ignoreDescendantsOf: List<String>,
) {
  fun analyze(klass: KtClass): KodeDiagnostic? {
    val isUiPackage = klass.containingKtFile.packageFqName.pathSegments().any { it.asString() == "ui" }
    if (!isUiPackage) return null
    if (!klass.isData() || klass.hasAnnotation("Immutable")) return null
    if (klass.superTypeNames().any { it in ignoreDescendantsOf }) return null
    return KodeDiagnostic("Data class \"${klass.name}\" is missing @Immutable annotation", klass)
  }

  private fun KtClass.superTypeNames(): List<String> = superTypeListEntries.flatMap { entry ->
    val type = entry.typeReference?.typeElement as? KtUserType ?: return@flatMap emptyList()
    val segments = generateSequence(type) { it.qualifier }.mapNotNull { it.referencedName }.toList().asReversed()
    val head = segments.firstOrNull() ?: return@flatMap emptyList()
    val written = segments.joinToString(".")
    val imported = containingKtFile.importDirectives
      .firstOrNull { it.importedName?.asString() == head }?.importedFqName?.asString()
    val packageName = containingKtFile.packageFqName.asString()
    val qualified = when {
      imported != null -> (listOf(imported) + segments.drop(1)).joinToString(".")
      packageName.isEmpty() -> written
      else -> "$packageName.$written"
    }
    // the last segment of the resolved name is the real short name, also behind an import alias
    listOf(written, qualified, qualified.substringAfterLast('.'))
  }
}
