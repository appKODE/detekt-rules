package sample

sealed interface Direction {
  object Up : Direction
  object Down : Direction
}

sealed class Shape {
  object Circle : Shape()
  class Square(val side: Int) : Shape()
}
