package ru.kode.detekt.rule.shared.analyzer

import org.jetbrains.kotlin.psi.KtFile
import ru.kode.detekt.rule.shared.KodeDiagnostic

/**
 * Checks that mapper files follow common naming patterns:
 *
 * * end with "Mappers" rather than "Mapper"
 */
class MapperFileNamingAnalyzer {
  fun analyze(file: KtFile): KodeDiagnostic? {
    if (!file.name.endsWith("Mapper.kt")) return null
    return KodeDiagnostic("File with mappers should have a name ending with \"Mappers\"", file)
  }
}
