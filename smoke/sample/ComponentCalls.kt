package sample

data class Person(val name: String, val age: Int)

fun describe(person: Person): String {
  val (name, age) = person
  return person.component1() + name + age
}
