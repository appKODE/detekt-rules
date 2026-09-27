package ru.kode.detekt.rule

import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import ru.kode.detekt.rule.contract.SharedRuleContracts
import java.util.ServiceLoader

class KodeRuleSetProviderTest : ShouldSpec({
  should("expose all kode rules") {
    val provider = KodeRuleSetProvider()
    val ruleSet = provider.instance()

    provider.ruleSetId shouldBe RuleSetId("kode")
    ruleSet.rules.size shouldBe SharedRuleContracts.expectedRuleIds.size
    ruleSet.rules.keys.map { it.value }.toSet() shouldBe SharedRuleContracts.expectedRuleIds
  }

  should("be discoverable through service loader") {
    val providers = ServiceLoader.load(RuleSetProvider::class.java).toList()
    providers.any { it is KodeRuleSetProvider } shouldBe true
  }
})
