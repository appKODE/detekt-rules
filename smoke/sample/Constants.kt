package sample

private const val PAGE_SIZE = 20

class Pager {
  companion object {
    const val FIRST_PAGE: Int = 0
  }

  fun pageSize(): Int = PAGE_SIZE
}

class OrderedPager {
  fun firstPage(): Int = LAST_PAGE

  companion object {
    const val LAST_PAGE: Int = 9
  }
}

private const val TIMEOUT = 5
