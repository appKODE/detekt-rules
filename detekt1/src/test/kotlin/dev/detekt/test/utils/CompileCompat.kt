package dev.detekt.test.utils

import org.jetbrains.kotlin.psi.KtFile
import io.github.detekt.test.utils.compileContentForTest as detekt1CompileContentForTest

fun compileContentForTest(content: String, filename: String = "Test.kt"): KtFile =
  detekt1CompileContentForTest(content, filename)
