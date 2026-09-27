package ru.kode.detekt.rule

import io.gitlab.arturbosch.detekt.api.RuleSetProvider
import io.gitlab.arturbosch.detekt.test.TestConfig
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import ru.kode.detekt.rule.contract.SharedRuleContracts
import java.util.ServiceLoader

class KodeRuleSetProviderTest : ShouldSpec({
  should("expose all kode rules") {
    val ruleSet = KodeRuleSetProvider().instance(TestConfig())

    ruleSet.id shouldBe "kode"
    ruleSet.rules.size shouldBe SharedRuleContracts.expectedRuleIds.size
    ruleSet.rules.map { it::class.simpleName!! }.toSet() shouldBe SharedRuleContracts.expectedRuleIds
  }

  should("be discoverable through service loader") {
    val providers = ServiceLoader.load(RuleSetProvider::class.java).toList()
    providers.any { it is KodeRuleSetProvider } shouldBe true
  }
})
