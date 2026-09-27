package ru.kode.detekt.rule.contract

object SharedRuleContracts {
  val expectedRuleIds: Set<String> = setOf(
    "RouteWiringMethodNaming",
    "MapperFileNaming",
    "PayloadArgumentName",
    "UseSurfaceModifier",
    "UseOnStartEmit",
    "ComponentFunctionCall",
    "BlockingSqlDelightCall",
    "ImmutableDataClass",
    "MissingTypeDeclaration",
  )
}
