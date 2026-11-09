package ru.kode.detekt.rule.shared.analyzer

import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtElement
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
 *
 * The finding is put on the `machine` name and lists the unhandled intents as `Intents.onItemClick`.
 *
 * The intents the intents class inherits are required too, and a reference counts wherever the view model reaches it:
 * in the class itself, in its superclasses and in the functions they call, of whatever file. Which intents classes the
 * view model answers for is taken from the class itself and its superclasses only: a helper function may refer to the
 * intents of another screen.
 */
class UnhandledViewIntentAnalyzer(
  private val semantic: KodeSemantic,
) {
  // The intents class is found through the `X::name` references of the view model, and an intent is a
  // property initialized with an `intent(...)` call. A view model without any such reference is not checked, and
  // neither is one whose `machine` is built by its superclass; an intent handled by a subclass, or by an object the
  // view model only passes the machine to, or by a function it passes by reference (`::configure`), is reported.
  // Take the intents class from the view model's type argument and resolve callable references to cover those
  fun analyze(klass: KtClass): KodeDiagnostic? {
    // a nested class is a view model of its own
    val machine = klass.findDescendantOfType<KtCallExpression> {
      it.calleeExpression?.text == "machine" && it.getStrictParentOfType<KtClass>() == klass
    } ?: return null
    val classes = listOf(klass) + semantic.sourceSuperClasses(klass)
    val references = handlingCode(classes).associateWith { code ->
      code.own<KtCallableReferenceExpression>().mapNotNull { reference ->
        reference.receiverName()?.let(semantic::referencedClass)?.to(reference.callableReference.getReferencedName())
      }
    }
    val handled = references.values.flatten()
    val hierarchies = classes.flatMap(references::getValue).map { it.first }.distinct()
      .associateWith { listOf(it) + semantic.sourceSuperClasses(it) }
    val unhandled = hierarchies
      // the intents of a base class are checked with the class that inherits them
      .filter { (intents, _) -> hierarchies.none { (other, hierarchy) -> other != intents && intents in hierarchy } }
      .flatMap { (intents, hierarchy) ->
        val names = handled.filter { it.first in hierarchy }.map { it.second }.toSet()
        (hierarchy.flatMap { it.intentNames() }.distinct() - names).map { "${intents.name}.$it" }
      }
    if (unhandled.isEmpty()) return null
    return KodeDiagnostic(
      "View intents are never handled in the machine of \"${klass.name}\": ${unhandled.joinToString()}",
      machine.calleeExpression ?: machine,
    )
  }

  /** The [classes] and the functions they call, directly or not. */
  private fun handlingCode(classes: List<KtClass>): Set<KtElement> {
    val code = classes.toMutableSet<KtElement>()
    val queue = ArrayDeque(code)
    while (queue.isNotEmpty()) {
      queue.removeFirst().own<KtCallExpression>()
        .mapNotNull(semantic::calledFunction)
        // a member of a class that is already there is read with that class
        .filterNot { it.getStrictParentOfType<KtClass>()?.let(code::contains) == true }
        .filter(code::add)
        .forEach(queue::add)
    }
    return code
  }

  /** The descendants of a function, or those of a class that are not in a class nested in it. */
  private inline fun <reified T : KtElement> KtElement.own(): List<T> = collectDescendantsOfType<T> {
    this !is KtClass || it.getStrictParentOfType<KtClass>() == this
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
