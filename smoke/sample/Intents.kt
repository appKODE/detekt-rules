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
