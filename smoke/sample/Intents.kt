package sample

class ScreenIntents {
  val onRefresh: Int = intent("refresh")
  val onBack: Int = intent("back")

  private fun intent(name: String): Int = name.length
}

fun machine(block: () -> Unit) {
  block()
}

fun handle(reference: Any) {
  println(reference)
}

class ScreenViewModel {
  fun buildMachine() {
    machine {
      handle(ScreenIntents::onRefresh)
    }
  }
}

open class BaseScreenIntents {
  val onClose: Int = intent()

  protected fun intent(): Int = 0
}

class DetailsIntents : BaseScreenIntents() {
  val onSubmit: Int = intent()
  val onHelp: Int = intent()
}

class DetailsViewModel {
  fun buildMachine() {
    machine {
      handle(DetailsIntents::onSubmit)
      configureHelp()
    }
  }
}
