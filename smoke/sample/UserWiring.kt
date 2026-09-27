package sample

class Coordinator {
  fun handleEvent(event: Any) {}
}

class UserWiring(private val coordinator: Coordinator) {
  fun openDetails() {
    coordinator.handleEvent("details")
  }

  fun navigateOnBack() {
    coordinator.handleEvent("back")
  }
}
