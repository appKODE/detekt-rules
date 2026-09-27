package ru.kode.detekt.rule

import dev.detekt.test.utils.createEnvironment
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.string.shouldStartWith
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelectorOrThis
import ru.kode.detekt.rule.shared.KodeSemantic

/**
 * One set of fixtures and expected answers for every [KodeSemantic] method, so the binding-context (detekt1)
 * and Analysis API (detekt2) implementations are verified to agree. Expected values are detekt1's answers.
 */
class KodeSemanticContractTest : ShouldSpec({
  val environment = createEnvironment()

  val renderedTypes by lazy {
    withKodeSemantic(environment, FIXTURE) { semantic, file ->
      file.collectDescendantsOfType<KtCallableDeclaration>()
        .filter { it.name?.startsWith("p") == true }
        .associate { it.name!! to semantic.renderedType(it) }
    }
  }

  fun <T> classAnswers(ask: KodeSemantic.(KtClass) -> T): Map<String, T> =
    withKodeSemantic(environment, FIXTURE) { semantic, file ->
      file.collectDescendantsOfType<KtClass>().associate { it.name!! to semantic.ask(it) }
    }

  should("render declaration types like detekt1's KotlinType.toString()") {
    renderedTypes.filterKeys { it !in KNOWN_DIFFERENCES && it !in UNRESOLVED } shouldContainExactly mapOf(
      "pInt" to "Int",
      "pNullable" to "String?",
      "pString" to "String",
      "pList" to "List<String>",
      "pMap" to "Map<Int, String>",
      "pMutableMap" to "MutableMap<Int, List<String?>>",
      "pTask" to "Task2<Int, String>",
      "pNested" to "Inner",
      "pInner" to "InnerBound",
      "pArrayOut" to "Array<out Number>",
      "pArrayInferred" to "Array<Int>",
      "pIntArray" to "IntArray",
      "pStar" to "List<*>",
      "pStarInferred" to "Box<*>",
      "pLambda" to "Function0<Int>",
      "pLambdaArg" to "Function1<Int, String>",
      "pSuspend" to "SuspendFunction0<Unit>",
      "pSuspendInferred" to "SuspendFunction0<Int>",
      "pExtension" to "[@kotlin.ExtensionFunctionType] Function1<Int, String>",
      "pExtensionInferred" to "[@kotlin.ExtensionFunctionType] Function1<Int, String>",
      "pAnnotatedFun" to "[@fixture.Marker] Function0<Unit>",
      "pAnnotated" to "[@fixture.Marker] Int",
      "pTypealias" to "Function1<Int, String>",
      "pTypealiasInferred" to "Function1<Int, String>",
      "pNamesAlias" to "List<String>",
      "pPlatform" to "(String..String?)",
      "pPlatformList" to "(MutableList<(String..String?)>..List<(String..String?)>?)",
      "pJavaClass" to "StringBuilder",
      "pTypeParam" to "T",
      "pTypeParamNullable" to "T?",
      "pNothing" to "Nothing",
      "pNullOnly" to "Nothing?",
      "pAny" to "Any",
      "pUnit" to "Unit",
      "pPair" to "Pair<Int, String>",
      "pAnonymous" to "Base",
      "pAnonymousPlain" to "Any",
      "pSequence" to "Sequence<Int>",
      "pFlexibleList" to "ArrayList<String>",
      "pIntersection" to "Any",
      "pVarargs" to "List<String>",
      "pLazy" to "Int",
      "pGetter" to "Int",
      "pVar" to "Long",
      "pFun" to "Int",
      "pFunUnit" to "Unit",
      "pFunGeneric" to "Box<List<Int>>",
      "pFunTypeParam" to "R",
      "pFunLambda" to "Function1<String, Int>",
      "pFunNothing" to "Nothing",
      "pFunNullable" to "Box<Int>?",
      "pFunSuspend" to "SuspendFunction0<String>",
      "pFunTwoArgs" to "Function2<Int, String, String>",
      "pFunReceiverLambda" to "[@kotlin.ExtensionFunctionType] Function2<String, Int, Char>",
      "pFunPairFun" to "Pair<Function1<Int, Unit>, Int?>",
      "pFunJava" to "HashMap<String, Int>",
      "pFunEnum" to "DeprecationLevel",
      "pFunChar" to "Char",
      "pFunArray" to "Array<String?>",
      "pFunFloat" to "Float",
      "pFunResult" to "Result<Int>",
      "pTwoAnnotations" to "[@fixture.Marker] [@fixture.Other] Int",
      "pAnnotatedExtension" to "[@fixture.Marker] [@kotlin.ExtensionFunctionType] Function1<Int, Unit>",
      "pNullableExtension" to "[@kotlin.ExtensionFunctionType] Function1<Int, Unit>?",
      "pSuspendExtension" to "[@kotlin.ExtensionFunctionType] SuspendFunction1<Int, Unit>",
      "pContravariant" to "Comparator<in String>",
      "pInnerGeneric" to "Inner2<String, Int>",
      "pNestedNullable" to "List<Map<String?, Int>?>?",
      "pAliasGeneric" to "Map<String, Int>",
      "pFunDnn" to "R & Any",
      "pFunDnnExplicit" to "R & Any",
      "pCompanion" to "Int",
    )
  }

  should("render unresolved types as error types; the error text differs per engine") {
    UNRESOLVED.forEach { renderedTypes.getValue(it)!! shouldStartWith "[Error type: " }
  }

  should("render the known differences as one of the two engines' answers") {
    KNOWN_DIFFERENCES.forEach { (name, answers) -> renderedTypes.getValue(name) shouldBeIn answers }
  }

  should("list all super classifier names, including the class itself") {
    classAnswers { superClassifierNames(it) } shouldContainExactly mapOf(
      "Marker" to setOf("Annotation", "Any", "Marker"),
      "Base" to setOf("Any", "Base"),
      "Derived" to setOf("Any", "Base", "Derived"),
      "Parent" to setOf("Any", "Base", "Derived", "Parent"),
      "Inner" to setOf("Any", "Inner"),
      "InnerBound" to setOf("Any", "InnerBound"),
      "Outer" to setOf("Any", "Outer"),
      "Task2" to setOf("Any", "Task2"),
      "Box" to setOf("Any", "Box"),
      "Other" to setOf("Annotation", "Any", "Other"),
      "Inner2" to setOf("Any", "Inner2"),
      "Outer2" to setOf("Any", "Outer2"),
      "Local" to setOf("Any", "Local"),
      "Probe" to setOf("Any", "Base", "Comparable", "Derived", "Parent", "Probe", "Serializable"),
    )
  }

  should("list direct super interface names") {
    classAnswers { directSuperInterfaceNames(it) } shouldContainExactly mapOf(
      "Marker" to setOf("Annotation"),
      "Base" to emptySet(),
      "Derived" to setOf("Base"),
      "Parent" to setOf("Derived"),
      "Inner" to emptySet(),
      "InnerBound" to emptySet(),
      "Outer" to emptySet(),
      "Task2" to emptySet(),
      "Box" to emptySet(),
      "Other" to setOf("Annotation"),
      "Inner2" to emptySet(),
      "Outer2" to emptySet(),
      "Local" to emptySet(),
      "Probe" to setOf("Comparable", "Serializable"),
    )
  }

  should("tell whether a call resolves to a member function of the given class or a subtype, except Any members") {
    withKodeSemantic(environment, CALL_FIXTURE) { semantic, file ->
      file.collectDescendantsOfType<KtCallExpression>()
        .associate {
          it.getQualifiedExpressionForSelectorOrThis().text to
            semantic.isMemberCallDeclaredInSubtypeOf(it, "calls.Transacter")
        }
    } shouldContainExactly mapOf(
      "emptyList()" to false,
      "UserQueries()" to false,
      "block()" to false,
      "q.byId(1)" to true,
      "q.selectAll()" to true,
      "q.transaction {}" to true,
      "q.toString()" to false,
      "q.equals(q)" to false,
      "s.toString()" to false,
      "s.select()" to true,
      "q.byId(\"x\")" to true,
      "q.missing()" to false,
      "TransacterImpl()" to false,
      "UserQueries.create()" to false,
      "q.ext()" to false,
      "t.transaction {}" to true,
      "a.byId(2)" to true,
      "o.other()" to false,
      "Other()" to false,
      "topLevel()" to false,
      "unknown()" to false,
      "withContext { q.selectAll() }" to false,
      "with(q) { byId(3) }" to false,
      "byId(3)" to true,
      "select()" to true,
      "\"b\".compareTo(\"a\")" to false,
      "d.daoSelect()" to true,
      "m.plain()" to true,
      "m.hashCode()" to false,
    )
  }
})

private val UNRESOLVED = setOf("pUnresolved", "pUnresolvedType", "pFunUnresolved")

// detekt1 answer first, detekt2 second
private val KNOWN_DIFFERENCES = mapOf(
  // explicitly declared, so never rendered by MissingTypeDeclaration; the Analysis API drops the `?`
  "pDnn" to setOf("T?", "T & Any"),
  // a public function returning a local class: the Analysis API approximates the type to its supertype
  "pFunLocalClass" to setOf("Local<T>", "Any"),
)

// language=kotlin
private val FIXTURE =
  """
    package fixture

    import java.io.Serializable

    typealias Handler = (Int) -> String
    typealias Names = List<String>
    annotation class Marker
    interface Base
    interface Derived : Base
    open class Parent : Derived
    class Outer { class Inner; inner class InnerBound }
    class Task2<A, B>
    class Box<T>(val value: T)
    annotation class Other
    class Outer2<A> { inner class Inner2<B> }
    typealias Mapping<V> = Map<String, V>

    class Probe<T : Any>(private val t: T) : Parent(), Serializable, Comparable<Probe<T>> {
      override fun compareTo(other: Probe<T>): Int = 0
      val pInt = 1
      val pNullable = if (t.hashCode() > 0) "a" else null
      val pString = "s"
      val pList = listOf("a")
      val pMap = mapOf(1 to "a")
      val pMutableMap = mutableMapOf<Int, List<String?>>()
      val pTask = Task2<Int, String>()
      val pNested = Outer.Inner()
      val pInner = Outer().InnerBound()
      val pArrayOut: Array<out Number> = arrayOf(1)
      val pArrayInferred = arrayOf(1, 2)
      val pIntArray = intArrayOf(1)
      val pStar = listOf<Any>() as List<*>
      val pStarInferred = Box<Any>(1) as Box<*>
      val pLambda = { 1 }
      val pLambdaArg = { id: Int -> id.toString() }
      val pSuspend: suspend () -> Unit = {}
      val pSuspendInferred = suspend { 1 }
      val pExtension: Int.() -> String = { toString() }
      val pExtensionInferred = fun Int.(): String = toString()
      val pAnnotatedFun: @Marker () -> Unit = {}
      val pAnnotated: @Marker Int = 1
      val pTypealias: Handler = { it.toString() }
      val pTypealiasInferred = pTypealias
      val pNamesAlias: Names = listOf()
      val pPlatform = System.getProperty("x")
      val pPlatformList = java.util.Collections.emptyList<String>()
      val pJavaClass = java.lang.StringBuilder()
      val pTypeParam = t
      val pTypeParamNullable: T? = null
      val pUnresolved = unknownCall()
      val pUnresolvedType = Unknown()
      val pNothing = throw IllegalStateException()
      val pNullOnly = null
      val pAny = Any()
      val pUnit = Unit
      val pPair = 1 to "a"
      val pAnonymous = object : Base {}
      val pAnonymousPlain = object {}
      val pSequence = sequenceOf(1).map { it * 2 }
      val pFlexibleList = java.util.ArrayList<String>()
      val pIntersection = if (t.hashCode() > 0) 1 else "a"
      val pDnn: (T & Any)? = null
      val pVarargs = listOf(*arrayOf("a"))
      val pLazy by lazy { 1 }
      val pGetter get() = 1
      var pVar = 1L
      fun pFun() = 1
      fun pFunUnit() {}
      fun pFunGeneric() = Box(listOf(1))
      fun <R> pFunTypeParam(r: R) = r
      fun pFunLambda() = { x: String -> x.length }
      fun pFunNothing() = TODO()
      fun pFunUnresolved() = unknownCall()
      fun pFunNullable() = if (t.hashCode() > 0) Box(1) else null
      fun pFunSuspend() = suspend { "a" }
      fun pFunTwoArgs() = { a: Int, b: String -> b + a }
      fun pFunReceiverLambda() = fun String.(i: Int): Char = this[i]
      fun pFunPairFun() = Pair<(Int) -> Unit, Int?>({}, null)
      fun pFunJava() = java.util.HashMap<String, Int>()
      fun pFunEnum() = kotlin.DeprecationLevel.ERROR
      fun pFunChar() = 'c'
      fun pFunArray() = arrayOfNulls<String>(1)
      fun pFunFloat() = 1f
      fun pFunResult() = runCatching { 1 }
      val pTwoAnnotations: @Marker @Other Int = 1
      val pAnnotatedExtension: @Marker Int.() -> Unit = {}
      val pNullableExtension: (Int.() -> Unit)? = null
      val pSuspendExtension: suspend Int.() -> Unit = {}
      val pContravariant: Comparator<in String> = compareBy { it }
      val pInnerGeneric = Outer2<Int>().Inner2<String>()
      val pNestedNullable: List<Map<String?, Int>?>? = null
      val pAliasGeneric: Mapping<Int> = mapOf()
      fun <R> pFunDnn(r: R) = r!!
      fun <R> pFunDnnExplicit(r: R & Any) = r
      fun pFunLocalClass() = run { class Local; Local() }
      companion object { val pCompanion = 1 }
    }
  """.trimIndent()

// language=kotlin
private val CALL_FIXTURE =
  """
    package calls

    interface Transacter { fun transaction(body: () -> Unit) {} }
    abstract class BaseQueries : Transacter { fun selectAll(): List<Int> = emptyList() }
    class UserQueries : BaseQueries() {
      fun byId(id: Int): Int = id
      companion object { fun create(): UserQueries = UserQueries() }
    }
    class TransacterImpl : Transacter
    class Other { fun other() {} }
    fun Transacter.ext() {}
    fun topLevel() {}
    fun <T> withContext(block: () -> T): T = block()
    interface UserDao { fun daoSelect(): Int }
    class DaoQueries : Transacter, UserDao { override fun daoSelect() = 0 }
    open class Plain { fun plain() {} }
    class Mixed : Plain(), Transacter

    abstract class Store : Transacter {
      override fun toString(): String = ""
      fun select() {}
      fun probe() { select(); "b".compareTo("a") }
    }

    fun <T : Transacter> probe(q: UserQueries, t: T, a: Any, o: Other, s: Store, d: DaoQueries, m: Mixed) {
      q.byId(1)
      q.equals(q)
      s.toString()
      s.select()
      q.byId("x")
      q.missing()
      q.selectAll()
      q.transaction {}
      q.toString()
      TransacterImpl()
      UserQueries.create()
      q.ext()
      t.transaction {}
      if (a is UserQueries) a.byId(2)
      o.other()
      Other()
      topLevel()
      unknown()
      withContext { q.selectAll() }
      with(q) { byId(3) }
      d.daoSelect()
      m.plain()
      m.hashCode()
    }
  """.trimIndent()
