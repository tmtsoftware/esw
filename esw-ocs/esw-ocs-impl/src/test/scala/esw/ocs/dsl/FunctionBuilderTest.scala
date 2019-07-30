package esw.ocs.dsl

import esw.ocs.BaseTestSuite
import esw.ocs.dsl.utils.FunctionBuilder

class FunctionBuilderTest extends BaseTestSuite {
  case class Command(name: String, value: Int)

  private val functionBuilder = new FunctionBuilder[Command, Int]
  functionBuilder.addHandler[Command](cmd => cmd.value * cmd.value)(_.name == "square")
  functionBuilder.addHandler[Command](cmd => Math.abs(cmd.value))(_.name == "abs")
  functionBuilder.addHandler[Command](cmd => 1 / cmd.value)(_.name == "reciprocal")

  private val builtFunctionWithHandlers = functionBuilder.build {
    case Command("die", _) => throw new RuntimeException("dead")
    case _                 => 0
  }

  "build " must {
    "execute matched handler and return Success | ESW-90" in {
      builtFunctionWithHandlers(Command("square", 10)) shouldBe 100
      builtFunctionWithHandlers(Command("abs", 25)) shouldBe 25
    }

    "execute default handler when no matching handler found | ESW-90" in {
      builtFunctionWithHandlers(Command("absent", 25)) shouldBe 0
    }

    "execute a handler and return Failure when there is an exception | ESW-90" in {
      an[ArithmeticException] shouldBe thrownBy(builtFunctionWithHandlers(Command("reciprocal", 0)))
    }
  }
}
