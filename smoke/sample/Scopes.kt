package sample

class CoroutineScope {
  fun cancel() {}
}

class LeakingPlayer {
  private val scope = CoroutineScope()
}

class ReleasedPlayer {
  private val scope = CoroutineScope()

  fun release() {
    scope.cancel()
  }
}
