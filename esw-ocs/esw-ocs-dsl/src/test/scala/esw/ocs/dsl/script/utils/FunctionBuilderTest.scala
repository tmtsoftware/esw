package esw.ocs.dsl.script.utils

import esw.testcommons.BaseTestSuite

class FunctionBuilderTest extends BaseTestSuite {
  case class Command(name: String, value: Int)

  private val functionBuilder = new FunctionBuilder[String, Command, Int]
  functionBuilder.add("square", cmd => cmd.value * cmd.value)
  functionBuilder.add("abs", cmd => Math.abs(cmd.value))
  functionBuilder.add("reciprocal", cmd => 1 / cmd.value)

  "contains should if handler is present or not" in {
    val square = Command("square", 10)
    functionBuilder.contains(square.name) shouldBe true

    val absent = Command("absent", 10)
    functionBuilder.contains(absent.name) shouldBe false
  }

  "execute matched handler and return Success | ESW-90" in {
    val square = Command("square", 10)
    functionBuilder.execute(square.name)(square) shouldBe 100

    val abs = Command("abs", 25)
    functionBuilder.execute(abs.name)(abs) shouldBe 25
  }

  "execute a handler and return Failure when there is an exception | ESW-90" in {
    val reciprocal = Command("reciprocal", 0)
    an[ArithmeticException] shouldBe thrownBy(functionBuilder.execute(reciprocal.name)(reciprocal))
  }

  "add should add handlers from other command handlers" in {
    val other = new FunctionBuilder[String, Command, Int]
    other.add("add2", cmd => cmd.value + 2)

    functionBuilder ++ other

    val cmd = Command("add2", 5)
    functionBuilder.contains(cmd.name) shouldBe true
    functionBuilder.execute(cmd.name)(cmd) shouldBe 7
  }
}
