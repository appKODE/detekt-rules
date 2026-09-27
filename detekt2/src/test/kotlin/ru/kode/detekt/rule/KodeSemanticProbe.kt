package ru.kode.detekt.rule

import dev.detekt.api.Config
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import dev.detekt.test.lintWithContext
import dev.detekt.test.utils.KotlinEnvironmentContainer
import org.jetbrains.kotlin.psi.KtFile
import ru.kode.detekt.rule.shared.KodeSemantic

/** Runs [block] with this engine's [KodeSemantic] inside the Analysis API session of a linted [code] file. */
fun <T> withKodeSemantic(
  environment: KotlinEnvironmentContainer,
  code: String,
  block: (KodeSemantic, KtFile) -> T,
): T {
  val probe = KodeSemanticProbe(block)
  probe.lintWithContext(environment, code)
  return probe.results.single()
}

private class KodeSemanticProbe<T>(
  private val block: (KodeSemantic, KtFile) -> T,
) : Rule(Config.empty, "Captures KodeSemantic answers"), RequiresAnalysisApi {
  val results = mutableListOf<T>()

  override fun visitKtFile(file: KtFile) {
    results += block(AnalysisApiKodeSemantic, file)
  }
}
