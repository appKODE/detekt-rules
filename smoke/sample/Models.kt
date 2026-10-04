package sample

open class ReactiveModel

class AccountModel : ReactiveModel() {
  suspend fun load(): Int = 1

  private suspend fun loadPrivately(): Int = 2

  fun start(): Int = 3
}
