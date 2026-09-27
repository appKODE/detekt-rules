package ru.kode.detekt.rule.shared.analyzer

import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.isPublic
import ru.kode.detekt.rule.shared.KodeDiagnostic
import ru.kode.detekt.rule.shared.KodeSemantic

data class MissingTypeDeclarationOptions(
  val ignoreInClassesDerivedFrom: List<String> = emptyList(),
  val ignoreInInterfacesDerivedFrom: List<String> = emptyList(),
  val ignorePropertiesOfType: List<Regex> = emptyList(),
)

/**
 * Suggests declaring an explicit type for public val/var/fun
 *
 * Wrong:
 *
 * ```
 * private val _privateStateFlow = MutableStateFlow<State>
 * val state = _privateStateFlow
 * fun myState() = _privateStateFlow
 * ```
 *
 * Correct:
 *
 * ```
 * val state: Flow<State> = _privateStateFlow
 * fun myState(): Flow<State> = _privateStateFlow
 * ```
 *
 * Checks declarations inside [KtClass] bodies, including nested classes and companion objects. Top-level
 * declarations, objects and local declarations are not checked.
 */
class MissingTypeDeclarationAnalyzer(
  private val options: MissingTypeDeclarationOptions,
  private val semantic: KodeSemantic,
) {
  fun analyze(klass: KtClass): List<KodeDiagnostic> {
    val superClasses = semantic.superClassifierNames(klass)
    val superInterfaces = semantic.directSuperInterfaceNames(klass)
    if (superClasses.any { it in options.ignoreInClassesDerivedFrom } ||
      superInterfaces.any { it in options.ignoreInInterfacesDerivedFrom }
    ) {
      return emptyList()
    }
    val diagnostics = mutableListOf<KodeDiagnostic>()
    klass.accept(
      object : KtTreeVisitorVoid() {
        override fun visitProperty(property: KtProperty) {
          // toString() keeps 1.x behaviour: an unresolved type renders as "null"
          val type = semantic.renderedType(property).toString()
          if (options.ignorePropertiesOfType.none { type.matches(it) } &&
            property.isPublic &&
            property.typeReference == null
          ) {
            diagnostics += KodeDiagnostic(
              "Missing explicit type declaration for public value '${property.name}: $type'",
              property,
            )
          }
          super.visitProperty(property)
        }

        override fun visitNamedFunction(function: KtNamedFunction) {
          if (function.isPublic) {
            val type = semantic.renderedType(function).toString()
            if (!function.hasDeclaredReturnType() && type != "Unit") {
              diagnostics += KodeDiagnostic(
                "Missing explicit type declaration for public function '${function.name}: $type'",
                function,
              )
            }
          }
          super.visitNamedFunction(function)
        }
      },
    )
    return diagnostics
  }
}
