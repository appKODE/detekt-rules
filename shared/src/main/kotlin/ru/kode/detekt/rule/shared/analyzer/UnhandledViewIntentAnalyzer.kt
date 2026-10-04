package ru.kode.detekt.rule.shared.analyzer

import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import ru.kode.detekt.rule.shared.KodeDiagnostic
import ru.kode.detekt.rule.shared.KodeSemantic

/**
 * Reports view intents that the view model never handles: a class that builds a `machine` and refers to some intents
 * of an intents class (`Intents::onRefresh`) must refer to every intent declared in that class.
 *
 * Wrong:
 *
 * ```
 * class Intents : ViewIntents() {
 *   val onRefresh = intent(name = "refresh")
 *   val onItemClick = intent<Item>(name = "itemClick")
 * }
 *
 * class MainViewModel : ViewModel<ViewState, Intents>() {
 *   override fun buildMachine(): Machine<ViewState> = machine {
 *     onEach(intent(Intents::onRefresh)) { action { _, _, _ -> model.fetch.start() } }
 *   }
 * }
 * ```
 *
 * Correct: the machine also has `onEach(intent(Intents::onItemClick)) { ... }`.
 */
class UnhandledViewIntentAnalyzer(
  private val semantic: KodeSemantic,
) {
  // The intents class is found through the `X::name` references of the view model, and an intent is a
  // property initialized with an `intent(...)` call in the body of X. A view model without any such reference is not
  // checked, intents inherited by X are not seen, and an intent handled outside the class that builds the machine
  // (a delegate, another file) is reported. Take the intents class from the view model's type argument to cover those
  fun analyze(klass: KtClass): KodeDiagnostic? {
    // a nested class is a view model of its own
    val machine = klass.findDescendantOfType<KtCallExpression> {
      it.calleeExpression?.text == "machine" && it.getStrictParentOfType<KtClass>() == klass
    } ?: return null
    val handled = klass.collectDescendantsOfType<KtCallableReferenceExpression>()
      .filter { it.getStrictParentOfType<KtClass>() == klass }
      .mapNotNull { reference ->
        reference.receiverName()?.let(semantic::referencedClass)?.to(reference.callableReference.getReferencedName())
      }
      .groupBy({ it.first }, { it.second })
    val unhandled = handled.flatMap { (intents, names) -> intents.intentNames() - names.toSet() }
    if (unhandled.isEmpty()) return null
    return KodeDiagnostic(
      "View intents are never handled in the machine of \"${klass.name}\": ${unhandled.joinToString()}",
      machine,
    )
  }

  private fun KtCallableReferenceExpression.receiverName(): KtNameReferenceExpression? =
    when (val receiver = receiverExpression) {
      is KtQualifiedExpression -> receiver.selectorExpression
      else -> receiver
    } as? KtNameReferenceExpression

  private fun KtClass.intentNames(): List<String> = body?.properties.orEmpty()
    .filter { (it.initializer as? KtCallExpression)?.calleeExpression?.text == "intent" }
    .mapNotNull { it.name }
}
