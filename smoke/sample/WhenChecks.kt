package sample

sealed interface LoadState {
  class Content(val items: Int) : LoadState
  object Loading : LoadState
  class Failed(val reason: String) : LoadState
}

fun render(state: LoadState): Int = when (state) {
  is LoadState.Content -> state.items
  LoadState.Loading -> 0
  is LoadState.Failed -> -1
}

enum class Color { Red }

fun describe(value: Any): Int = when (value) {
  is String -> 1
  Color.Red -> 2
  else -> 3
}

fun renderConsistently(state: LoadState): Int = when (state) {
  is LoadState.Content -> state.items
  is LoadState.Loading -> 0
  is LoadState.Failed -> -1
}
