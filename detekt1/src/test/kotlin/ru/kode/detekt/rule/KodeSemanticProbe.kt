package ru.kode.detekt.rule

import dev.detekt.test.utils.KotlinEnvironmentContainer
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import io.gitlab.arturbosch.detekt.test.lintWithContext
import org.jetbrains.kotlin.psi.KtFile
import ru.kode.detekt.rule.shared.KodeSemantic

/** Runs [block] with this engine's [KodeSemantic], bound to a type-resolved [code] file. */
fun <T> withKodeSemantic(
  environment: KotlinEnvironmentContainer,
  code: String,
  block: (KodeSemantic, KtFile) -> T,
): T {
  val results = mutableListOf<T>()
  val probe = object : Rule(Config.empty) {
    override val issue = Issue("KodeSemanticProbe", Severity.Defect, "", Debt.FIVE_MINS)

    override fun visitKtFile(file: KtFile) {
      results += block(BindingContextKodeSemantic(bindingContext), file)
    }
  }
  probe.lintWithContext(environment.env, code)
  return results.single()
}
