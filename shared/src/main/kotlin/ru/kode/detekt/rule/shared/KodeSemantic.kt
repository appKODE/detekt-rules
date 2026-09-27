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
   * Whether [call] resolves, even with errors, to a member function (not a constructor) of the class [classFqName]
   * or a subtype of it, as seen from the receiver: inherited members and overrides count, like in 1.x, except
   * members that ultimately override a `kotlin.Any` member (`toString()`, `equals()`...).
   */
  fun isMemberCallDeclaredInSubtypeOf(call: KtCallExpression, classFqName: String): Boolean
}
