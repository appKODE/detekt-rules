package sample

fun printLength(text: String?) {
  text?.let { println(it.length) }
}

fun lengthOf(text: String?): Int = text?.let { it.length } ?: 0

fun printAll(texts: List<String?>) {
  texts.forEach { text ->
    text?.let { println(it) }
  }
}

fun lengths(texts: List<String?>): List<Int> = texts.map { text -> text?.let { it.length } ?: 0 }
