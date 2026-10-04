package ru.kode.detekt.rule

import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

class KodeRuleSetProvider : RuleSetProvider {
  override val ruleSetId: RuleSetId = RuleSetId("kode")

  override fun instance(): RuleSet {
    return RuleSet(
      ruleSetId,
      listOf(
        { config -> RouteWiringMethodNaming(config) },
        { config -> MapperFileNaming(config) },
        { config -> PayloadArgumentName(config) },
        { config -> UseSurfaceModifier(config) },
        { config -> UseOnStartEmit(config) },
        { config -> ComponentFunctionCall(config) },
        { config -> BlockingSqlDelightCall(config) },
        { config -> ImmutableDataClass(config) },
        { config -> MissingTypeDeclaration(config) },
        { config -> NullableValueClassProperty(config) },
        { config -> ConstantsNotAtBottom(config) },
        { config -> InconsistentWhenTypeCheck(config) },
        { config -> SealedClassCouldBeEnum(config) },
        { config -> UnusedLetResult(config) },
        { config -> CoroutineScopeNotCancelled(config) },
        { config -> CancellationExceptionNotRethrown(config) },
        { config -> PublicSuspendFunctionInReactiveModel(config) },
      ),
    )
  }
}
