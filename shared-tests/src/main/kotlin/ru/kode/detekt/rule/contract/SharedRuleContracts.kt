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
    "NullableValueClassProperty",
    "ConstantsNotAtBottom",
    "InconsistentWhenTypeCheck",
    "SealedClassCouldBeEnum",
    "UnusedLetResult",
    "CoroutineScopeNotCancelled",
    "CancellationExceptionNotRethrown",
    "PublicSuspendFunctionInReactiveModel",
    "UnhandledViewIntent",
  )
}
