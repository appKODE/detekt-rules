package ru.kode.detekt.rule

import dev.detekt.test.lint
import dev.detekt.test.utils.compileContentForTest
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

class MapperFileNamingTest : ShouldSpec({
  val code = """
    package cases

    fun Throwable.toSomeModel() = Unit
  """.trimIndent()

  should("report error if mapper file does not end with Mappers") {
    val finding = MapperFileNaming().lint(compileContentForTest(code, "SomeFeatureMapper.kt")).single()

    finding.message shouldBe "File with mappers should have a name ending with \"Mappers\""
    "${finding.entity.location.source.line}:${finding.entity.location.source.column}" shouldBe "1:1"
  }

  should("report no error if mapper file ends with Mappers") {
    MapperFileNaming().lint(compileContentForTest(code, "SomeFeatureMappers.kt")).shouldBeEmpty()
  }

  should("report a file named just Mapper") {
    MapperFileNaming().lint(compileContentForTest(code, "Mapper.kt")).single()
  }

  should("not report other file names") {
    MapperFileNaming().lint(compileContentForTest(code, "MapperUtils.kt")).shouldBeEmpty()
    MapperFileNaming().lint(compileContentForTest(code, "SomeFeaturemapper.kt")).shouldBeEmpty()
  }

  should("report an empty mapper file") {
    MapperFileNaming().lint(compileContentForTest("", "SomeFeatureMapper.kt")).single()
  }
})
