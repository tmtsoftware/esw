package esw.ocs.api.models

import csw.params.commands.{CommandName, Setup}
import csw.prefix.models.Prefix
import esw.ocs.api.models
import esw.ocs.api.models.StepStatus.*
import esw.ocs.api.protocol.EditorError.CannotOperateOnAnInFlightOrFinishedStep
import org.scalatest.EitherValues.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class StepTest extends AnyWordSpecLike with Matchers {

  "apply" must {
    "create new step from provided command" in {
      val setup = Setup(Prefix("esw.test"), CommandName("test"), None)

      val step = Step(setup)
      step.command should ===(setup)
      step.status should ===(Pending)
      step.hasBreakpoint should ===(false)
    }
  }

  "isPending" must {
    "return true when step status is Pending" in {
      val setup = Setup(Prefix("esw.test"), CommandName("test"), None)
      val step  = Step(setup, Pending, hasBreakpoint = false)
      step.isPending should ===(true)
      step.isInFlight should ===(false)
      step.isFinished should ===(false)
    }
  }

  "isInFlight" must {
    "return true when step status is InFlight" in {
      val setup = Setup(Prefix("esw.test"), CommandName("test"), None)
      val step  = Step(setup, InFlight, hasBreakpoint = false)
      step.isInFlight should ===(true)
      step.isPending should ===(false)
      step.isFinished should ===(false)
    }
  }

  "isFinished" must {

    "return true when step status is Finished" in {
      val setup = Setup(Prefix("esw.test"), CommandName("test"), None)
      val step  = models.Step(setup, Finished.Success, hasBreakpoint = false)
      step.isFinished should ===(true)
      step.isInFlight should ===(false)
      step.isPending should ===(false)
    }
  }

  "addBreakpoint" must {
    "add breakpoint when step status is Pending | ESW-106" in {
      val setup      = Setup(Prefix("esw.test"), CommandName("test"), None)
      val step       = Step(setup, Pending, hasBreakpoint = false)
      val stepResult = step.addBreakpoint()
      stepResult.toOption.get.hasBreakpoint should ===(true)
    }

    "fail with CannotOperateOnAnInFlightOrFinishedStep error when step status is InFlight | ESW-106" in {
      val setup      = Setup(Prefix("esw.test"), CommandName("test"), None)
      val step       = Step(setup, InFlight, hasBreakpoint = false)
      val stepResult = step.addBreakpoint()
      stepResult.left.value should ===(CannotOperateOnAnInFlightOrFinishedStep)
    }

    "fail with CannotOperateOnAnInFlightOrFinishedStep error when step status is Finished | ESW-106" in {
      val setup      = Setup(Prefix("esw.test"), CommandName("test"), None)
      val step       = models.Step(setup, Finished.Success, hasBreakpoint = false)
      val stepResult = step.addBreakpoint()
      stepResult.left.value should ===(CannotOperateOnAnInFlightOrFinishedStep)
    }
  }

  "removeBreakpoint" must {
    "remove the breakpoint | ESW-107" in {
      val setup       = Setup(Prefix("esw.test"), CommandName("test"), None)
      val step        = Step(setup, Pending, hasBreakpoint = true)
      val updatedStep = step.removeBreakpoint()
      updatedStep.hasBreakpoint should ===(false)
    }

    "be no op when step does not have breakpoint | ESW-107" in {
      val setup       = Setup(Prefix("esw.test"), CommandName("test"), None)
      val step        = Step(setup, Pending, hasBreakpoint = false)
      val updatedStep = step.removeBreakpoint()
      updatedStep should ===(step)
    }
  }

  "withStatus" must {
    "change the status to InFlight from Pending" in {
      val setup      = Setup(Prefix("esw.test"), CommandName("test"), None)
      val step       = Step(setup, Pending, hasBreakpoint = true)
      val stepResult = step.withStatus(InFlight)
      stepResult.status should ===(InFlight)
    }

    "change the status to Finished from InFlight " in {
      val setup            = Setup(Prefix("esw.test"), CommandName("test"), None)
      val step             = Step(setup, InFlight, hasBreakpoint = true)
      val finishedResponse = Finished.Success
      val stepResult       = step.withStatus(finishedResponse)
      stepResult.status should ===(finishedResponse)
    }
  }
}
