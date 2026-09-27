package ru.kode.detekt.rule

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.config
import io.gitlab.arturbosch.detekt.api.internal.RequiresTypeResolution
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.resolve.BindingContext
import ru.kode.detekt.rule.shared.analyzer.MissingTypeDeclarationAnalyzer
import ru.kode.detekt.rule.shared.analyzer.MissingTypeDeclarationOptions

@RequiresTypeResolution
class MissingTypeDeclaration(
  config: Config = Config.empty,
  /** If property parent is derived from class in this list, the rule will skip this property */
  ignoreInClassesDerivedFrom: List<String> = emptyList(),
  /** If property parent is derived from interface in this list, the rule will skip this property */
  ignoreInInterfacesDerivedFrom: List<String> = emptyList(),
  /** If property has the type specified in this list, the rule will skip it. Can be plain string or regexp */
  ignorePropertiesOfType: List<String> = emptyList(),
) : Detekt1SharedRule(config, "Suggests declaring an explicit type for public val/var/fun") {

  private val ignoreInClassesDerivedFrom by config(defaultValue = ignoreInClassesDerivedFrom)
  private val ignoreInInterfacesDerivedFrom by config(defaultValue = ignoreInInterfacesDerivedFrom)
  private val ignorePropertiesOfType by config(
    defaultValue = ignorePropertiesOfType,
    transformer = { it.map { type -> Regex(type) } },
  )

  override fun visitClass(klass: KtClass) {
    // no super call: nested classes are checked as part of the outer class, like 1.x did
    if (bindingContext == BindingContext.EMPTY) return
    // bindingContext is set per file, so the analyzer cannot be cached
    val analyzer = MissingTypeDeclarationAnalyzer(
      MissingTypeDeclarationOptions(ignoreInClassesDerivedFrom, ignoreInInterfacesDerivedFrom, ignorePropertiesOfType),
      BindingContextKodeSemantic(bindingContext),
    )
    reportDiagnostics(analyzer.analyze(klass))
  }
}
