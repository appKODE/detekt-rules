package ru.kode.detekt.rule.shared

import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtClass

interface KodeSemantic {
  /** Simple names of the class itself plus all its transitive supertypes (classes and interfaces, incl. `Any`). */
  fun superClassifierNames(klass: KtClass): Set<String>

  /** Simple names of the direct super-interfaces only. */
  fun directSuperInterfaceNames(klass: KtClass): Set<String>

  /**
   * Type of a property or return type of a function, rendered like detekt1's `KotlinType.toString()`
   * (simple class names, `FunctionN<...>`, flexible types as `(A..A?)`); null if it cannot be resolved.
   */
  fun renderedType(declaration: KtCallableDeclaration): String?

  /**
   * Whether [call] resolves to a member or constructor whose owner is the class [classFqName] or a subtype of it.
   * The owner is the class the member was resolved on, like detekt1's containing declaration of the resolved call:
   * the static type of the dispatch receiver (so inherited `Any` members count too), or the constructed class.
   */
  fun isMemberCallOnSubtypeOf(call: KtCallExpression, classFqName: String): Boolean
}
