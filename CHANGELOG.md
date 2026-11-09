# Changelog

## 2.1.0 - 2026-11-09

Nine new rules. All of them are inactive by default: enable the ones you need in your detekt config.

Six of them need type resolution (marked below). Those report nothing in a plain `detekt` task of detekt 1 and
without `--analysis-mode full` on detekt 2, see "Type resolution" in the README. No rule of 2.0.0 changes its
behaviour or its requirements.

* `NullableValueClassProperty`: a value class wrapping a nullable type
* `ConstantsNotAtBottom`: `const val`s declared above other declarations of a file, or in a companion object that is
  not the last declaration of its class
* `InconsistentWhenTypeCheck`: a `when` mixing `is` checks with equality checks of objects (needs type resolution)
* `SealedClassCouldBeEnum`: a sealed class or interface whose inheritors are all stateless objects (needs type
  resolution)
* `UnusedLetResult`: a `let { }` call used as a statement (needs type resolution)
* `CoroutineScopeNotCancelled`: a class that creates a coroutine scope and never cancels it
* `CancellationExceptionNotRethrown`: a `catch` of `CancellationException` or of a broad type around a suspending
  call that swallows `CancellationException` (needs type resolution)
* `PublicSuspendFunctionInReactiveModel`: a public `suspend` function of a reactive model (needs type resolution)
* `UnhandledViewIntent`: view intents that the machine of a view model never handles (needs type resolution)

Two related checks are not added, because detekt already ships them in its `style` rule set: `UseEmptyCounterpart`
(`emptyList()` instead of `listOf()`) and `UnnecessaryLet`.

## 2.0.0 - 2026-09-27

First open-source release, published to Maven Central.

* Two artifacts with the same rules, rule set id, rule ids and configuration keys:
  `ru.kode:detekt-rules` for detekt 1.22.0 - 1.23.8 and `ru.kode:detekt-rules-detekt2` for detekt 2.0.0-alpha.6
* A default `config.yml` is shipped inside both jars
* Rules added since 1.4.0: `MissingTypeDeclaration` (active by default) and `ComponentFunctionCall` (inactive by
  default)
* Fixed rules reporting too much or too little, see below
* The jars have no runtime dependencies and target Java 11

### Upgrading from 1.4.0

1. The coordinates changed from `ru.kode.detekt.rules:detekt-rules` to `ru.kode:detekt-rules`, which requires
   detekt 1.22.0 or later (`ru.kode:detekt-rules-detekt2` for detekt 2.0.0-alpha.6). If your project
   committed the rules jar (`kode-android-rules-1.4.0.jar` or `detekt-rules-1.4.0.jar`), delete it and use the Maven
   dependency instead:

   ```kotlin
   dependencies {
     detektPlugins("ru.kode:detekt-rules:2.0.0")
   }
   ```

2. The rule set id (`kode`), the rule ids and the configuration keys are unchanged, so your `detekt-config.yml` keeps
   working. With `buildUponDefaultConfig = true`, rules missing from it take their defaults from the shipped
   `config.yml`; otherwise add the new rules to it to enable them:

   | Rule | New since 1.4.0 | Default |
   |---|---|---|
   | `MissingTypeDeclaration` | yes | active |
   | `ComponentFunctionCall` | yes | inactive |
   | `UseOnStartEmit` | no | inactive |
   | all other rules | no | active |

3. Expect a few new or removed findings from these fixes:

   * `RouteWiringMethodNaming`: methods declared after a nested class, a local class or an enum entry of a wiring
     class are now checked (new findings)
   * `UseOnStartEmit`, `ComponentFunctionCall`: every match in a call chain and in its lambdas is reported, not only
     the outermost one (new findings)
   * `UseSurfaceModifier`: `Modifier` chains nested in another chain and composables declared inside a
     non-composable function are now checked (new findings)
   * `PayloadArgumentName`: a payload inside nested `onEach` calls is reported once instead of once per `onEach`
     (fewer findings)
   * `BlockingSqlDelightCall`: a call in a nested suspend function is reported once instead of twice; a
     `withContext(...)` enclosing a local function now covers the calls inside it; constructor calls of `Transacter`
     subtypes and `Any` members (`toString()`, `hashCode()`, `equals()`) are no longer reported (fewer findings)
   * `ImmutableDataClass`: `ignoreDescendantsOf` entries now also match a fully qualified supertype name, written in
     the declaration or resolved through an import or the file's package (fewer findings)

## 1.5.0 - 2025-09-26

* Added the `MissingTypeDeclaration` rule (internal release only)

## 1.4.0 - 2023-09-29

* Added the `ImmutableDataClass` rule

## 1.3.0 - 2023-09-13

* Added the `BlockingSqlDelightCall` rule

## 1.2.0 - 2022-10-04

* Added the `UseOnStartEmit` rule

## 1.1.0 - 2022-05-19

* Removed the Compose rules, which moved to [detekt-rules-compose](https://github.com/appKODE/detekt-rules-compose)
* Removed the `MissingPropsInitialization` rule, obsolete after the move to Compose

## 1.0.6 - 2022-04-20

* Added the `UseSurfaceModifier` rule

## 1.0.5 - 2022-04-15

* Added the `UnnecessaryEventHandlerParameter` rule

## 1.0.4 - 2022-03-29

* `ModifierOnWrongLevel` detects more cases
* Added the `ComposableEventParameterNaming` rule

## 1.0.3 - 2022-03-14

* Added the `PayloadArgumentName` rule

## 1.0.2 - 2021-12-17

* Fixed an incompatibility with detekt 1.19.0, see [detekt#4228](https://github.com/detekt/detekt/issues/4228)

## 1.0.1 - 2021-10-14

* Fixed the path of the `RuleSetProvider` file in `META-INF`

## 1.0.0 - 2021-10-14

* Initial release
