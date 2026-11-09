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

class BlockReleasedPlayer {
  private val scope = CoroutineScope()

  fun release() {
    with(scope) { cancel() }
  }
}

abstract class BasePlayer {
  protected val scope = CoroutineScope()
  private val hidden = CoroutineScope()
}
