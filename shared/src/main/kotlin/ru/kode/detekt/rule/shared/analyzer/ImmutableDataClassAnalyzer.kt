package ru.kode.detekt.rule.shared.analyzer

import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.getSuperNames
import ru.kode.detekt.rule.shared.KodeDiagnostic
import ru.kode.detekt.rule.shared.hasAnnotation

/**
 * Reports data classes declared in a `ui` package (any package segment) without the `@Immutable` annotation.
 * Descendants of the classes listed in [ignoreDescendantsOf] are skipped; only direct supertype names written in
 * the declaration are considered.
 */
class ImmutableDataClassAnalyzer(
  private val ignoreDescendantsOf: List<String>,
) {
  fun analyze(klass: KtClass): KodeDiagnostic? {
    val isUiPackage = klass.containingKtFile.packageFqName.pathSegments().any { it.asString() == "ui" }
    if (!isUiPackage) return null
    if (!klass.isData() || klass.hasAnnotation("Immutable")) return null
    if (klass.getSuperNames().any { it in ignoreDescendantsOf }) return null
    return KodeDiagnostic("Data class \"${klass.name}\" is missing @Immutable annotation", klass)
  }
}
