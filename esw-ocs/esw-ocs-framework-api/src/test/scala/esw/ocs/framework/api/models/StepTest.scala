package esw.ocs.framework.api.models

import csw.params.commands.CommandResponse.Completed
import csw.params.commands.{CommandName, Setup}
import csw.params.core.models.{Id, Prefix}
import esw.ocs.framework.api.BaseTestSuite
import esw.ocs.framework.api.models.StepStatus.{Finished, InFlight, Pending}
import esw.ocs.framework.api.models.messages.StepListError.{AddingBreakpointNotSupported, UpdateNotSupported}

class StepTest extends BaseTestSuite {
  def finished(id: Id) = Finished.Success(Completed(id))

  "apply" must {
    "create new step from provided command" in {
      val setup = Setup(Prefix("test"), CommandName("test"), None)

      val step = Step(setup)
      step.command shouldBe setup
      step.status shouldBe Pending
      step.hasBreakpoint shouldBe false
    }
  }

  "id" must {
    "be same as provided sequence commands Id" in {
      val setup = Setup(Prefix("test"), CommandName("test"), None)
      val step  = Step(setup)
      step.id shouldBe setup.runId
    }
  }

  "isPending" must {
    "return true when step status is Pending" in {
      val setup = Setup(Prefix("test"), CommandName("test"), None)
      val step  = Step(setup, Pending, hasBreakpoint = false)
      step.isPending shouldBe true
      step.isInFlight shouldBe false
      step.isFinished shouldBe false
    }
  }

  "isInFlight" must {
    "return true when step status is InFlight" in {
      val setup = Setup(Prefix("test"), CommandName("test"), None)
      val step  = Step(setup, InFlight, hasBreakpoint = false)
      step.isInFlight shouldBe true
      step.isPending shouldBe false
      step.isFinished shouldBe false
    }
  }

  "isFinished" must {

    "return true when step status is Finished" in {
      val setup = Setup(Prefix("test"), CommandName("test"), None)
      val step  = Step(setup, finished(setup.runId), hasBreakpoint = false)
      step.isFinished shouldBe true
      step.isInFlight shouldBe false
      step.isPending shouldBe false
    }
  }

  "addBreakpoint" must {
    "add breakpoint when step status is Pending" in {
      val setup      = Setup(Prefix("test"), CommandName("test"), None)
      val step       = Step(setup, Pending, hasBreakpoint = false)
      val stepResult = step.addBreakpoint()
      stepResult.right.value.hasBreakpoint shouldBe true
    }

    "not not add breakpoint when step status is InFlight" in {
      val setup      = Setup(Prefix("test"), CommandName("test"), None)
      val step       = Step(setup, InFlight, hasBreakpoint = false)
      val stepResult = step.addBreakpoint()
      stepResult.left.value shouldBe AddingBreakpointNotSupported(InFlight)
    }

    "not not add breakpoint when step status is Finished" in {
      val setup      = Setup(Prefix("test"), CommandName("test"), None)
      val step       = Step(setup, finished(setup.runId), hasBreakpoint = false)
      val stepResult = step.addBreakpoint()
      stepResult.left.value shouldBe AddingBreakpointNotSupported(finished(setup.runId))
    }
  }

  "removeBreakpoint" must {
    "remove the breakpoint" in {
      val setup       = Setup(Prefix("test"), CommandName("test"), None)
      val step        = Step(setup, Pending, hasBreakpoint = true)
      val updatedStep = step.removeBreakpoint()
      updatedStep.hasBreakpoint shouldBe false
    }

    "be no op when step does not have breakpoint" in {
      val setup       = Setup(Prefix("test"), CommandName("test"), None)
      val step        = Step(setup, Pending, hasBreakpoint = false)
      val updatedStep = step.removeBreakpoint()
      updatedStep shouldBe step
    }
  }

  "withStatus" must {
    "change the status to InFlight from Pending" in {
      val setup      = Setup(Prefix("test"), CommandName("test"), None)
      val step       = Step(setup, Pending, hasBreakpoint = true)
      val stepResult = step.withStatus(InFlight)
      stepResult.right.value.status shouldBe InFlight
    }

    "change the status to Finished from InFlight " in {
      val setup            = Setup(Prefix("test"), CommandName("test"), None)
      val step             = Step(setup, InFlight, hasBreakpoint = true)
      val finishedResponse = finished(setup.runId)
      val stepResult       = step.withStatus(finishedResponse)
      stepResult.right.value.status shouldBe finishedResponse
    }

    "fail for invalid status transitions" in {
      val setup      = Setup(Prefix("test"), CommandName("test"), None)
      val step       = Step(setup, InFlight, hasBreakpoint = true)
      val stepResult = step.withStatus(Pending)
      stepResult.left.value shouldBe UpdateNotSupported(InFlight, Pending)
    }
  }
}
