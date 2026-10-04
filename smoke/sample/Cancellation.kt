package sample

fun rethrowCancellation(error: Throwable) {
  if (error is InterruptedException) throw error
}

suspend fun swallow(action: suspend () -> Unit) {
  try {
    action()
  } catch (e: Exception) {
    println(e)
  }
}

suspend fun rethrow(action: suspend () -> Unit) {
  try {
    action()
  } catch (e: Exception) {
    rethrowCancellation(e)
    println(e)
  }
}
