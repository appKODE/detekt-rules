package ru.kode.detekt.rule

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

class KodeRuleSetProvider : RuleSetProvider {
  override val ruleSetId = "kode"

  override fun instance(config: Config): RuleSet {
    return RuleSet(
      ruleSetId,
      listOf(
        RouteWiringMethodNaming(config),
        MapperFileNaming(config),
        PayloadArgumentName(config),
        UseSurfaceModifier(config),
        UseOnStartEmit(config),
        ComponentFunctionCall(config),
        BlockingSqlDelightCall(config),
        ImmutableDataClass(config),
        MissingTypeDeclaration(config),
      ),
    )
  }
}
