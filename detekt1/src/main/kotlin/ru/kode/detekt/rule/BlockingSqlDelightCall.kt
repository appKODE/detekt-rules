package ru.kode.detekt.rule

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.config
import io.gitlab.arturbosch.detekt.api.internal.RequiresTypeResolution
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.resolve.BindingContext
import ru.kode.detekt.rule.shared.analyzer.BlockingSqlDelightCallAnalyzer

@RequiresTypeResolution
class BlockingSqlDelightCall(
  config: Config = Config.empty,
  sqlDelightPackage: String = "app.cash.sqldelight",
) : Detekt1SharedRule(config, "Reports SqlDelight DB calls not made within the coroutine context") {

  private val sqlDelightPackage by config(defaultValue = sqlDelightPackage)

  // bindingContext is set per file
  private var analyzer: BlockingSqlDelightCallAnalyzer? = null

  override fun visitKtFile(file: KtFile) {
    analyzer = BlockingSqlDelightCallAnalyzer(sqlDelightPackage, BindingContextKodeSemantic(bindingContext))
      .takeIf { bindingContext != BindingContext.EMPTY }
    super.visitKtFile(file)
  }

  override fun visitNamedFunction(function: KtNamedFunction) {
    analyzer?.let { reportDiagnostics(it.analyze(function)) }
    super.visitNamedFunction(function)
  }
}
