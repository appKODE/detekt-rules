# KODE detekt rules

A set of [detekt](https://detekt.dev) rules for Android projects that follow the conventions used at KODE: Compose UI,
coroutines `Flow`s, SqlDelight, mappers and route wiring classes. Compose-specific rules live in the separate
[detekt-rules-compose](https://github.com/appKODE/detekt-rules-compose) rule set, and both can be used together.

# Installation

Pick the artifact that matches your detekt version:

```kotlin
dependencies {
  // detekt 1.22.0 - 1.23.8
  detektPlugins("ru.kode:detekt-rules:2.1.0")

  // detekt 2.0.0-alpha.6
  detektPlugins("ru.kode:detekt-rules-detekt2:2.1.0")
}
```

Both artifacts contain the same rules with the same rule set id (`kode`), rule ids and configuration keys, so
`detekt-config.yml` does not change when you switch engines. The rule jars have no dependencies: they run on the
Kotlin compiler bundled with detekt.

Both artifacts ship a default configuration. With `buildUponDefaultConfig = true` you only need to override what
differs; otherwise copy this section to your `detekt-config.yml`:

```yaml
kode:
  active: true
  RouteWiringMethodNaming:
    active: true
  MapperFileNaming:
    active: true
  PayloadArgumentName:
    active: true
  UseSurfaceModifier:
    active: true
  UseOnStartEmit:
    active: false
  ComponentFunctionCall:
    active: false
  BlockingSqlDelightCall:
    active: true
    sqlDelightPackage: 'app.cash.sqldelight'
  ImmutableDataClass:
    active: true
    ignoreDescendantsOf: []
  MissingTypeDeclaration:
    active: true
    ignoreInClassesDerivedFrom: []
    ignoreInInterfacesDerivedFrom: []
    ignorePropertiesOfType: []
  NullableValueClassProperty:
    active: false
  ConstantsNotAtBottom:
    active: false
  InconsistentWhenTypeCheck:
    active: false
  SealedClassCouldBeEnum:
    active: false
  UnusedLetResult:
    active: false
  CoroutineScopeNotCancelled:
    active: false
  CancellationExceptionNotRethrown:
    active: false
    rethrowFunctions: []
  PublicSuspendFunctionInReactiveModel:
    active: false
    baseClasses: ['ReactiveModel']
  UnhandledViewIntent:
    active: false
```

# Rules

| Rule | Default | What it reports | Options (default) |
|---|---|---|---|
| `RouteWiringMethodNaming` | active | A method of a class whose name contains `Wiring` that calls `coordinator.handleEvent` but is not named `navigateOn...` | — |
| `MapperFileNaming` | active | A file named `*Mapper.kt`: mapper files should end with `Mappers` | — |
| `PayloadArgumentName` | active | A lambda parameter named `payload` in `transitionTo { state, payload -> }` or `action { _, _, payload -> }` inside `onEach(...) { }` | — |
| `UseSurfaceModifier` | active | A `Modifier` chain in a `@Composable` function combining `background(shape = ...)` with `clickable`, `clip` or `shadow`; use `Modifier.surface()` | — |
| `UseOnStartEmit` | inactive | `flow.onStart { emit(value) }`; use `flow.onStartEmit(value)` | — |
| `ComponentFunctionCall` | inactive | A direct `componentN()` call; use property access or destructuring | — |
| `BlockingSqlDelightCall` | active | A call of a SqlDelight `Transacter` member (the database or its queries) in a `suspend` function outside `withContext(...)` | `sqlDelightPackage` (`app.cash.sqldelight`): package of `Transacter` |
| `ImmutableDataClass` | active | A data class in a `ui` package (any package segment) without `@Immutable` | `ignoreDescendantsOf` (`[]`): skip data classes whose direct supertype has one of these short or fully qualified names |
| `MissingTypeDeclaration` | active | A public property without an explicit type, or a public non-`Unit` function without an explicit return type, in a class body | `ignoreInClassesDerivedFrom` (`[]`): skip classes with one of these (transitive) supertypes, by simple name; `ignoreInInterfacesDerivedFrom` (`[]`): skip classes that directly implement one of these interfaces, by simple name; `ignorePropertiesOfType` (`[]`): skip properties whose type matches one of these regular expressions |
| `NullableValueClassProperty` | inactive | A `value class` whose property has a nullable type; make it non-null and use a nullable value class instead | — |
| `ConstantsNotAtBottom` | inactive | A top-level `const val` followed by anything but properties, or a companion object with `const val`s that is not the last member of its class; one finding per file and per class | — |
| `InconsistentWhenTypeCheck` | inactive | In a `when (subject)` that has `is` branches, a branch comparing the subject to an object by equality (`State.Loading ->`): a plain or qualified name that resolves to an `object` declaration. Constants, enum entries and companion objects are not reported. Needs type resolution | — |
| `SealedClassCouldBeEnum` | inactive | A sealed class or interface without type parameters and without a superclass whose inheritors (at least two, in whatever file they are declared) are all objects with no state of their own. Needs type resolution | — |
| `UnusedLetResult` | inactive | A `let { }` call in statement position, where its result is discarded (also inside `if` / `when` / `try` branches and loop bodies of a statement, and as the last statement of a lambda that returns `Unit`, like `forEach { it?.let { } }`). Needs type resolution | — |
| `CoroutineScopeNotCancelled` | inactive | A class property initialized with `CoroutineScope(...)` or `MainScope()` when the class itself (not a nested class) has no `cancel` call on it (`scope.cancel()`, `scope.coroutineContext[Job]?.cancel()`, or `job.cancel()` for `CoroutineScope(job)`). Cancelling the result of a call (`scope.launch { }.cancel()`) does not count. Objects are skipped: they live as long as the process | — |
| `CancellationExceptionNotRethrown` | inactive | A `catch` of `CancellationException`, `Throwable`, `Exception`, `RuntimeException` or `IllegalStateException` that neither rethrows the caught exception nor calls `ensureActive()`, unless an earlier `catch (e: CancellationException)` rethrows, when its `try` block calls a `suspend` function: directly or in a lambda of an inline function (`forEach { }`), in a `suspend` function as well as in a suspend lambda (`scope.launch { try { } catch ... }`). A `try` that suspends only through an operator, a `for` loop or a delegated property is not reported, and the caught types are matched by name. Needs type resolution. detekt 2 ships a similar check, `SuspendFunSwallowedCancellation`; in detekt 1 that rule covers `runCatching` only | `rethrowFunctions` (`[]`): names of functions whose call in the `catch` counts as a rethrow |
| `PublicSuspendFunctionInReactiveModel` | inactive | A public, non-`override` `suspend` function declared in a class derived (directly or not) from one of `baseClasses`. Needs type resolution | `baseClasses` (`['ReactiveModel']`): simple names of the reactive model base classes |
| `UnhandledViewIntent` | inactive | A class that calls `machine` and refers to intents as `Intents::onRefresh`, when some property initialized with `intent(...)` of that intents class or of its superclasses is never referred to. A reference counts in the class itself, in its superclasses and in the functions they call, of any file. One finding per class, put on the `machine` name and listing the unhandled intents as `Intents.onBack`. Needs type resolution | — |

# Type resolution

`BlockingSqlDelightCall`, `MissingTypeDeclaration`, `InconsistentWhenTypeCheck`, `SealedClassCouldBeEnum`,
`UnusedLetResult`, `CancellationExceptionNotRethrown`, `PublicSuspendFunctionInReactiveModel` and
`UnhandledViewIntent`
need type information; the other rules work on syntax only.

- detekt 1: they only report in the [type resolution](https://detekt.dev/docs/gettingstarted/type-resolution)
  tasks (`detektMain`, `detektTest`, or `detekt-cli --classpath ...`). In a plain `detekt` task they report nothing.
- detekt 2: they only run with `--analysis-mode full`.

# Credits

The rules were written at KODE by Dmitry Suzdalev, Oleg Terekhov and Roman Chetverikov in KODE's internal
`detekt-rules` repository (versions 1.x), and ported from it for this open-source release.

# License

```
Copyright 2021 KODE LLC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
