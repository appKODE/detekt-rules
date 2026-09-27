package ru.kode.detekt.rule.shared

import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtUserType

/** Visits selectors from the outermost to the innermost one, then the final receiver. */
internal inline fun KtDotQualifiedExpression.forEachSubExpression(action: (KtExpression) -> Unit) {
  var e: KtExpression? = this
  while (e != null) {
    val selector = (e as? KtDotQualifiedExpression)?.selectorExpression
    if (selector != null) {
      action(selector)
      e = (e as KtDotQualifiedExpression).receiverExpression
    } else {
      action(e)
      e = null
    }
  }
}

/** Same matching as detekt 1.x `hasAnnotation`: the last segment of the annotation's user type. */
internal fun KtAnnotated.hasAnnotation(name: String): Boolean =
  annotationEntries.any { (it.typeReference?.typeElement as? KtUserType)?.referencedName == name }

/** Strict parents of type [T], stopping at the first [S] (like detekt 1.x `parentsOfTypeUntil`). */
internal inline fun <reified T : KtElement, reified S : KtElement> KtElement.parentsOfTypeUntil(): Sequence<T> =
  generateSequence(parent) { it.parent }.takeWhile { it !is S }.filterIsInstance<T>()
