# KODE detekt rules

A set of [detekt](https://detekt.dev) rules for Android projects that follow the conventions used at KODE: Compose UI,
coroutines `Flow`s, SqlDelight, mappers and route wiring classes. Compose-specific rules live in the separate
[detekt-rules-compose](https://github.com/appKODE/detekt-rules-compose) rule set, and both can be used together.

# Installation

Pick the artifact that matches your detekt version:

```kotlin
dependencies {
  // detekt 1.22.0 - 1.23.8
  detektPlugins("ru.kode:detekt-rules:2.0.0")

  // detekt 2.0.0-alpha.6
  detektPlugins("ru.kode:detekt-rules-detekt2:2.0.0")
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

# Type resolution

`BlockingSqlDelightCall` and `MissingTypeDeclaration` need type information; the other rules work on syntax only.

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
