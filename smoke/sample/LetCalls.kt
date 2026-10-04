package sample

fun printLength(text: String?) {
  text?.let { println(it.length) }
}

fun lengthOf(text: String?): Int = text?.let { it.length } ?: 0
