package ru.kode.detekt.rule.shared

import org.jetbrains.kotlin.psi.KtElement

data class KodeDiagnostic(
  val message: String,
  val anchor: KtElement,
)
