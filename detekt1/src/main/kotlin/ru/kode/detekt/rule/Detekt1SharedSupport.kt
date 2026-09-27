package ru.kode.detekt.rule

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperClassifiers
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperInterfaces
import ru.kode.detekt.rule.shared.KodeDiagnostic
import ru.kode.detekt.rule.shared.KodeSemantic

abstract class Detekt1SharedRule(
  config: Config,
  description: String,
) : Rule(config) {
  override val issue: Issue = Issue(
    javaClass.simpleName,
    Severity.Defect,
    description,
    Debt.FIVE_MINS,
  )

  internal fun reportDiagnostics(diagnostics: List<KodeDiagnostic>) {
    diagnostics.forEach { diagnostic ->
      report(CodeSmell(issue, Entity.from(diagnostic.anchor), diagnostic.message))
    }
  }
}

internal class BindingContextKodeSemantic(
  private val bindingContext: BindingContext,
) : KodeSemantic {

  override fun superClassifierNames(klass: KtClass): Set<String> = bindingContext[BindingContext.CLASS, klass]
    ?.getAllSuperClassifiers()
    .orEmpty()
    .mapTo(mutableSetOf()) { it.typeConstructor.toString() }

  override fun directSuperInterfaceNames(klass: KtClass): Set<String> = bindingContext[BindingContext.CLASS, klass]
    ?.getSuperInterfaces()
    .orEmpty()
    .mapTo(mutableSetOf()) { it.typeConstructor.toString() }

  override fun renderedType(declaration: KtCallableDeclaration): String? {
    val descriptor = when (declaration) {
      is KtProperty -> bindingContext[BindingContext.VARIABLE, declaration]
      is KtNamedFunction -> bindingContext[BindingContext.FUNCTION, declaration]
      else -> null
    }
    return descriptor?.returnType?.toString()
  }

  override fun isMemberCallOnSubtypeOf(call: KtCallExpression, classFqName: String): Boolean {
    val owner = call.getResolvedCall(bindingContext)?.resultingDescriptor?.containingDeclaration as? ClassDescriptor
    return owner?.getAllSuperClassifiers().orEmpty().any { it.fqNameSafe.asString() == classFqName }
  }
}
