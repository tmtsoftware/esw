package esw.ocs.framework.dsl

import esw.ocs.framework.BaseTestSuite
import esw.ocs.framework.dsl.internal.FunctionBuilder

import scala.util.Success

class FunctionBuilderTest extends BaseTestSuite {

  "build" when {
    "handlers are empty" should {
      "execute default case and return Success" in {
        val functionBuilder = new FunctionBuilder[Int, Int]
        functionBuilder.build(default = _ + 10)(10) shouldBe Success(20)
      }
    }
    "handlers are added" when {
      case class Command(name: String, value: Int)
      val functionBuilder = new FunctionBuilder[Command, Int]

      functionBuilder.addHandler[Command](cmd ⇒ cmd.value * cmd.value)(_.name == "square")
      functionBuilder.addHandler[Command](cmd ⇒ Math.abs(cmd.value))(_.name == "abs")
      functionBuilder.addHandler[Command](cmd ⇒ 1 / cmd.value)(_.name == "reciprocal")
      val builtFunction = functionBuilder.build {
        case Command("die", _) ⇒ throw new RuntimeException("dead")
        case _                 ⇒ 0
      }

      "found matching handler" when {
        "there is no exception" should {
          "execute matched handler and return Success" in {
            builtFunction(Command("square", 10)) shouldBe Success(100)
            builtFunction(Command("abs", 25)) shouldBe Success(25)
          }
        }
        "there is an exception" should {
          "execute matched handler and return Failure" in {
            val result = builtFunction(Command("reciprocal", 0))
            an[ArithmeticException] shouldBe thrownBy(result.get)
          }
        }
      }
      "not found matching handler" when {
        "there is no exception" should {
          "execute default handler and return Success" in {
            builtFunction(Command("absent", 25)) shouldBe Success(0)

          }
        }
        "there is an exception" should {
          "execute default handler and return Failure" in {
            val result = builtFunction(Command("die", 10))
            an[RuntimeException] shouldBe thrownBy(result.get)
          }
        }
      }
    }
  }
}
