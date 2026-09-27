package ru.kode.detekt.rule

import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.resolution.singleFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaDefinitelyNotNullType
import org.jetbrains.kotlin.analysis.api.types.KaErrorType
import org.jetbrains.kotlin.analysis.api.types.KaFlexibleType
import org.jetbrains.kotlin.analysis.api.types.KaFunctionType
import org.jetbrains.kotlin.analysis.api.types.KaStarTypeProjection
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeArgumentWithVariance
import org.jetbrains.kotlin.analysis.api.types.KaTypeParameterType
import org.jetbrains.kotlin.analysis.api.types.KaTypeProjection
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.types.Variance
import ru.kode.detekt.rule.shared.KodeDiagnostic
import ru.kode.detekt.rule.shared.KodeSemantic

internal object AnalysisApiKodeSemantic : KodeSemantic {
  override fun superClassifierNames(klass: KtClass): Set<String> = analyze(klass) {
    val symbol = klass.classSymbol ?: return@analyze emptySet()
    symbol.defaultType.allSupertypes.mapNotNullTo(mutableSetOf(symbol.name.toString())) { it.symbol?.name?.asString() }
  }

  override fun directSuperInterfaceNames(klass: KtClass): Set<String> = analyze(klass) {
    klass.classSymbol?.superTypes.orEmpty()
      .mapNotNull { it.symbol as? KaClassSymbol }
      .filter { it.classKind == KaClassKind.INTERFACE }
      .mapNotNullTo(mutableSetOf()) { it.name?.asString() }
  }

  override fun isMemberCallDeclaredInSubtypeOf(call: KtCallExpression, classFqName: String): Boolean = analyze(call) {
    val applied = call.resolveToCall()?.singleFunctionCallOrNull()?.partiallyAppliedSymbol ?: return@analyze false
    val function = applied.symbol
    // detekt1 resolves an inherited member to a fake override owned by the receiver's class: use the receiver type
    val owner = applied.dispatchReceiver?.type
    if (function is KaConstructorSymbol || owner == null) return@analyze false
    val original = function.fakeOverrideOriginal
    (owner.allSupertypes + owner).any { (it as? KaClassType)?.classId?.asSingleFqName()?.asString() == classFqName } &&
      (original.allOverriddenSymbols + original)
        .filter { it.directlyOverriddenSymbols.none() }
        .none { (it.containingDeclaration as? KaClassSymbol)?.classId?.asSingleFqName()?.asString() == "kotlin.Any" }
  }

  override fun renderedType(declaration: KtCallableDeclaration): String? = analyze(declaration) {
    render(declaration.returnType)
  }
}

/** Renders [type] like detekt1's (K1) `KotlinType.toString()`. */
private fun KaSession.render(type: KaType): String = when (type) {
  is KaFlexibleType -> "(${render(type.lowerBound)}..${render(type.upperBound)})"

  // K1 simplifies `(T & Any)?` to `T?`
  is KaDefinitelyNotNullType -> render(type.original) + if (type.isMarkedNullable) "?" else " & Any"

  // the texts differ from K1 ("Not found recorded type..."), only the prefix is shared
  is KaErrorType -> "[Error type: $type]"

  is KaClassType -> buildString {
    type.annotations.forEach { append("[@").append(it.classId?.asFqNameString()).append("] ") }
    // K1 marks receiver function types with an annotation the Analysis API models as a property
    if (type is KaFunctionType && type.hasReceiver) append("[@kotlin.ExtensionFunctionType] ")
    append(type.classId.shortClassName.asString())
    // K1 lists an inner class's own type arguments first, then its outer classes'
    val arguments = type.qualifiers.asReversed().flatMap { it.typeArguments }
    if (arguments.isNotEmpty()) arguments.joinTo(this, ", ", "<", ">") { render(it) }
    if (type.isMarkedNullable) append("?")
  }

  is KaTypeParameterType -> type.name.asString() + if (type.isMarkedNullable) "?" else ""

  else -> type.toString()
}

private fun KaSession.render(projection: KaTypeProjection): String = when (projection) {
  is KaStarTypeProjection -> "*"

  is KaTypeArgumentWithVariance -> when (projection.variance) {
    Variance.INVARIANT -> render(projection.type)
    else -> "${projection.variance.label} ${render(projection.type)}"
  }
}

internal fun Rule.reportDiagnostics(diagnostics: List<KodeDiagnostic>) {
  diagnostics.forEach { diagnostic ->
    report(Finding(Entity.from(diagnostic.anchor), diagnostic.message))
  }
}
