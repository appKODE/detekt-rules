package ru.kode.detekt.rule

import dev.detekt.test.lint
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class UseSurfaceModifierTest : ShouldSpec({
  should("report usage of shaped background with clickable") {
    val code = """
      @Composable
      fun Test() {
        Column(
          modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .clip(shape = shape)
            .border(
              width = 1.dp,
              color = borderColor,
              shape = shape
            )
            .background(color = backgroundColor, shape = shape)
            .padding(vertical = 6.dp, horizontal = 16.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        )
      }
    """.trimIndent()

    val finding = UseSurfaceModifier().lint(code).single()

    finding.message shouldBe
      "Use \"Modifier.surface()\" instead of combining shaped background with clickable/clip/shadow"
    finding.shouldStartAt(code, "Modifier\n")
  }

  should("report usage of shaped background with clip") {
    val code = """
      @Composable
      fun Test() {
        Box(
          modifier = Modifier
            .background(color = Color.Red, shape = RoundCornerShape(12.dp))
            .clip(RoundCornerShape(12.dp)),
        )
      }
    """.trimIndent()

    UseSurfaceModifier().lint(code) shouldHaveSize 1
  }

  should("report usage of shaped background with shadow") {
    val code = """
      @Composable
      fun Test() {
        Box(
          modifier = modifier
            .shadow(12.dp, RoundCornerShape(12.dp))
            .background(color = Color.Red, shape = RoundCornerShape(12.dp)))
      }
    """.trimIndent()

    UseSurfaceModifier().lint(code).single().shouldStartAt(code, "modifier\n")
  }

  should("report usage of shaped background with shape extracted to val") {
    val code = """
      @Composable
      fun Test(modifier: Modifier) {
        val shape = RoundCornerShape(12.dp)
        Box(
          modifier = modifier.shadow(elevation = 12.dp, shape = shape).clickable(onClick = shape)
            .clip(shape)
            .background(color = Color.Red, shape = shape))
      }
    """.trimIndent()

    UseSurfaceModifier().lint(code) shouldHaveSize 1
  }

  should("not report background without shape") {
    val code = """
      @Composable
      fun Test() {
        Box(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .background(color = Color.Red)
            .clickable(onClick = {})
            .shadow(shape = RoundCornerShape(12.dp))
            .clip(shape = RoundCornerShape(24.dp))
        )
      }
    """.trimIndent()

    UseSurfaceModifier().lint(code).shouldBeEmpty()
  }

  should("not report shaped background alone or with other modifiers") {
    val code = """
      @Composable
      fun Test() {
        Box(modifier = Modifier.background(color = Color.Red, shape = shape).padding(4.dp).border(1.dp, shape = shape))
        Box(modifier = Modifier.background(Color.Red, shape).clip(shape))
      }
    """.trimIndent()

    UseSurfaceModifier().lint(code).shouldBeEmpty()
  }

  should("not report chains outside of composable functions") {
    val code = """
      fun notComposable() = Modifier.background(color = Color.Red, shape = shape).clip(shape)

      val property = Modifier.background(color = Color.Red, shape = shape).clip(shape)
    """.trimIndent()

    UseSurfaceModifier().lint(code).shouldBeEmpty()
  }

  should("report every chain of composables, class members and fully qualified annotations included") {
    val code = """
      class Screen {
        @androidx.compose.runtime.Composable
        fun Content() {
          Box(modifier = Modifier.background(color = Color.Red, shape = shape).clip(shape))
          Box(modifier = Modifier.clickable {}.background(color = Color.Red, shape = shape))
        }
      }
    """.trimIndent()

    UseSurfaceModifier().lint(code) shouldHaveSize 2
  }

  // 1.x skipped the chains nested in another chain and the composables nested in a non-composable function
  should("check nested chains and nested functions, each chain once") {
    val code = """
      @Composable
      fun Outer() {
        fun helper() = Modifier.background(color = Color.Red, shape = helperShape).clip(helperShape)
        Box(modifier = Modifier.then(Modifier.background(color = Color.Red, shape = innerShape).clip(innerShape)))

        @Composable
        fun NestedComposable() {
          Box(modifier = Modifier.shadow(1.dp).background(color = Color.Red, shape = nestedShape))
        }
      }

      fun notComposable() {
        @Composable
        fun Inner() {
          Box(modifier = Modifier.clickable {}.background(color = Color.Red, shape = localShape))
        }
      }
    """.trimIndent()

    val findings = UseSurfaceModifier().lint(code).sortedBy { it.entity.location.source.line }

    findings shouldHaveSize 4
    findings[0].shouldStartAt(code, "Modifier.background(color = Color.Red, shape = helperShape)")
    findings[1].shouldStartAt(code, "Modifier.background(color = Color.Red, shape = innerShape)")
    findings[2].shouldStartAt(code, "Modifier.shadow")
    findings[3].shouldStartAt(code, "Modifier.clickable")
  }
})
