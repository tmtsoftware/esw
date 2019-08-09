package esw.ocs.api.models

import csw.params.commands.CommandResponse.Completed
import csw.params.commands.{CommandName, Setup}
import csw.params.core.models.{Id, Prefix}
import esw.ocs.api.models.StepStatus.{Finished, _}
import esw.ocs.api.models.messages.EditorError.{NotSupported, UpdateNotSupported}
import esw.ocs.api.{BaseTestSuite, models}

class StepTest extends BaseTestSuite {
  def finished(id: Id) = Finished.Success(Completed(id))

  "apply" must {
    "create new step from provided command" in {
      val setup = Setup(Prefix("esw.test"), CommandName("test"), None)

      val step = Step(setup)
      step.command should ===(setup)
      step.status should ===(Pending)
      step.hasBreakpoint should ===(false)
    }
  }

  "id" must {
    "be same as provided sequence commands Id" in {
      val setup = Setup(Prefix("esw.test"), CommandName("test"), None)
      val step  = Step(setup)
      step.id should ===(setup.runId)
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
      val step  = models.Step(setup, finished(setup.runId), hasBreakpoint = false)
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

    "fail with NotSupported error when step status is InFlight | ESW-106" in {
      val setup      = Setup(Prefix("esw.test"), CommandName("test"), None)
      val step       = Step(setup, InFlight, hasBreakpoint = false)
      val stepResult = step.addBreakpoint()
      stepResult.left.value should ===(NotSupported(InFlight))
    }

    "fail with NotSupported error when step status is Finished | ESW-106" in {
      val setup      = Setup(Prefix("esw.test"), CommandName("test"), None)
      val step       = models.Step(setup, finished(setup.runId), hasBreakpoint = false)
      val stepResult = step.addBreakpoint()
      stepResult.left.value should ===(NotSupported(finished(setup.runId)))
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
      stepResult.toOption.get.status should ===(InFlight)
    }

    "change the status to Finished from InFlight " in {
      val setup            = Setup(Prefix("esw.test"), CommandName("test"), None)
      val step             = Step(setup, InFlight, hasBreakpoint = true)
      val finishedResponse = finished(setup.runId)
      val stepResult       = step.withStatus(finishedResponse)
      stepResult.toOption.get.status should ===(finishedResponse)
    }

    "fail for invalid status transitions" in {
      val setup      = Setup(Prefix("esw.test"), CommandName("test"), None)
      val step       = Step(setup, InFlight, hasBreakpoint = true)
      val stepResult = step.withStatus(Pending)
      stepResult.left.value should ===(UpdateNotSupported(InFlight, Pending))
    }
  }
}
