package ru.kode.detekt.rule.shared

import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

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

  /**
   * The class [reference] refers to (`Intents` in `Intents::onClick`), when it is declared in the analyzed sources;
   * null when the reference is unresolved, refers to anything else (an object, an enum entry, a property, a type
   * alias...) or to a class of a library.
   */
  fun referencedClass(reference: KtNameReferenceExpression): KtClass?

  /**
   * Whether [reference] refers to an `object` declaration (`Loading` in `State.Loading`), of the analyzed sources or
   * of a library. A companion object referred to by the name of its class, an enum entry, a property and an
   * unresolved reference are not objects.
   */
  fun isObjectReference(reference: KtNameReferenceExpression): Boolean

  /**
   * Whether the lambda [literal] returns `Unit`, so the value of its last statement is discarded: its expected type
   * says so (`forEach { }`, `val action: () -> Unit = { }`) or it is inferred from the last statement. A lambda whose
   * type is not resolved does not return `Unit`.
   */
  fun returnsUnit(literal: KtFunctionLiteral): Boolean

  /**
   * Declarations of the direct inheritors of the sealed class or interface [klass], in whatever file of the analyzed
   * sources they are; empty when [klass] is not sealed.
   */
  fun sealedInheritors(klass: KtClass): List<KtClassOrObject>

  /**
   * Whether [call] resolves to a `suspend` function, the `invoke` of a value of a suspend function type included
   * (`action()` for `action: suspend () -> Unit`).
   */
  fun isSuspendCall(call: KtCallExpression): Boolean

  /** Whether [call] resolves to an `inline` function, so its lambda arguments run in the place of the call. */
  fun isInlineCall(call: KtCallExpression): Boolean
}
