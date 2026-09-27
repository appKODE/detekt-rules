package ru.kode.detekt.rule

import dev.detekt.api.Config
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtClass
import ru.kode.detekt.rule.shared.analyzer.MissingTypeDeclarationAnalyzer
import ru.kode.detekt.rule.shared.analyzer.MissingTypeDeclarationOptions

class MissingTypeDeclaration(
  config: Config = Config.empty,
  /** If property parent is derived from class in this list, the rule will skip this property */
  ignoreInClassesDerivedFrom: List<String> = emptyList(),
  /** If property parent is derived from interface in this list, the rule will skip this property */
  ignoreInInterfacesDerivedFrom: List<String> = emptyList(),
  /** If property has the type specified in this list, the rule will skip it. Can be plain string or regexp */
  ignorePropertiesOfType: List<String> = emptyList(),
) : Rule(config, "Suggests declaring an explicit type for public val/var/fun"), RequiresAnalysisApi {

  private val ignoreInClassesDerivedFrom by config(defaultValue = ignoreInClassesDerivedFrom)
  private val ignoreInInterfacesDerivedFrom by config(defaultValue = ignoreInInterfacesDerivedFrom)
  private val ignorePropertiesOfType by config(
    defaultValue = ignorePropertiesOfType,
    transformer = { it.map { type -> Regex(type) } },
  )

  private val analyzer by lazy {
    MissingTypeDeclarationAnalyzer(
      MissingTypeDeclarationOptions(
        this.ignoreInClassesDerivedFrom,
        this.ignoreInInterfacesDerivedFrom,
        this.ignorePropertiesOfType,
      ),
      AnalysisApiKodeSemantic,
    )
  }

  override fun visitClass(klass: KtClass) {
    // no super call: nested classes are checked as part of the outer class, like 1.x did
    reportDiagnostics(analyzer.analyze(klass))
  }
}
