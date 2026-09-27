package sample

import app.cash.sqldelight.Transacter

suspend fun <T> withContext(context: Any, block: suspend () -> T): T = block()

class UserRepository(private val db: Transacter) {
  suspend fun load() {
    db.transaction {}
  }

  suspend fun loadOnIo() {
    withContext(Unit) {
      db.transaction {}
    }
  }

  fun loadBlocking() {
    db.transaction {}
  }
}
