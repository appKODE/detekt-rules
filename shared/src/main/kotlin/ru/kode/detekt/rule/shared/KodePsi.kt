package ru.kode.detekt.rule.shared

import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtUserType

/**
 * The parts of a qualified chain owned by this link alone: its selector, plus the head receiver for the innermost
 * link. A visitor that visits every link of a chain sees each part exactly once.
 */
internal fun KtDotQualifiedExpression.ownSubExpressions(): List<KtExpression> =
  listOfNotNull(receiverExpression.takeIf { it !is KtDotQualifiedExpression }, selectorExpression)

/** Same matching as detekt 1.x `hasAnnotation`: the last segment of the annotation's user type. */
internal fun KtAnnotated.hasAnnotation(name: String): Boolean =
  annotationEntries.any { (it.typeReference?.typeElement as? KtUserType)?.referencedName == name }
