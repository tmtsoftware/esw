package esw.ocs.api.models

import csw.params.commands._
import csw.params.core.models.Id
import csw.prefix.models.Prefix
import esw.ocs.api.models
import esw.ocs.api.models.StepStatus.{Finished, InFlight, Pending}
import esw.ocs.api.protocol.EditorError.{CannotOperateOnAnInFlightOrFinishedStep, IdDoesNotExist}
import org.scalatest.EitherValues._
import org.scalatest.OptionValues._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class StepListTest extends AnyWordSpecLike with Matchers {

  "apply" must {
    "return a StepList" in {
      val setup1 = Setup(Prefix("esw.ocs.move1"), CommandName("test1"), None)
      val setup2 = Setup(Prefix("esw.ocs.move2"), CommandName("test2"), None)

      val stepList = StepList(Sequence(setup1, setup2))

      stepList.steps.length should ===(2)
    }
  }

  "isFinished" must {
    val setup1 = Setup(Prefix("esw.ocs.move1"), CommandName("test1"), None)
    val setup2 = Setup(Prefix("esw.ocs.move2"), CommandName("test2"), None)

    "return false when StepList is empty" in {
      val stepList = StepList(Nil)
      stepList.isFinished should ===(false)
    }

    "return true when all steps are Finished" in {
      val step1 = Step(setup1, Finished.Success, hasBreakpoint = false)
      val step2 = models.Step(setup2, Finished.Success, hasBreakpoint = false)

      val stepList = StepList(List(step1, step2))
      stepList.isFinished should ===(true)
    }

    "return true when any step is Failed" in {
      val step1 = Step(setup1, Finished.Failure(), hasBreakpoint = false)
      val step2 = Step(setup2, InFlight, hasBreakpoint = false)

      val stepList = StepList(List(step1, step2))
      stepList.isFinished should ===(true)
    }

    "return false when any step is not Finished" in {
      val step1 = models.Step(setup1, Finished.Success, hasBreakpoint = false)
      val step2 = Step(setup2, InFlight, hasBreakpoint = false)

      val stepList = StepList(List(step1, step2))
      stepList.isFinished should ===(false)
    }
  }

  "isPaused" must {
    val setup1 = Setup(Prefix("esw.ocs.move1"), CommandName("test1"), None)
    val setup2 = Setup(Prefix("esw.ocs.move2"), CommandName("test2"), None)

    "return true when next step exists and it has a breakpoint" in {
      val step1 = Step(setup1, InFlight, hasBreakpoint = false)
      val step2 = Step(setup2, Pending, hasBreakpoint = true)

      val stepList = StepList(List(step1, step2))
      stepList.isPaused should ===(true)
    }

    "return false when next step exists but doesn't have breakpoint" in {
      val step1 = Step(setup1, InFlight, hasBreakpoint = false)
      val step2 = Step(setup2, Pending, hasBreakpoint = false)

      val stepList = StepList(List(step1, step2))
      stepList.isPaused should ===(false)
    }

    "return false when next step doesn't exist" in {
      val step1 = Step(setup1, InFlight, hasBreakpoint = false)

      val stepList = StepList(List(step1))
      stepList.isPaused should ===(false)
    }
  }

  "isInFlight" must {
    val setup1 = Setup(Prefix("esw.ocs.move1"), CommandName("test1"), None)
    val setup2 = Setup(Prefix("esw.ocs.move2"), CommandName("test2"), None)

    "return true when at least one InFlight step exist" in {
      val step1 = Step(setup1, InFlight, hasBreakpoint = false)
      val step2 = Step(setup2, Pending, hasBreakpoint = true)

      val stepList = StepList(List(step1, step2))
      stepList.isInFlight should ===(true)
    }

    "return false when no InFlight step exist" in {
      val step1 = models.Step(setup1, Finished.Success, hasBreakpoint = false)
      val step2 = Step(setup2, Pending, hasBreakpoint = false)

      val stepList = StepList(List(step1, step2))
      stepList.isInFlight should ===(false)
    }
  }

  "nextPending" must {
    "return next pending step" in {
      val setup1 = Setup(Prefix("esw.ocs.move1"), CommandName("test1"), None)
      val setup2 = Setup(Prefix("esw.ocs.move2"), CommandName("test2"), None)
      val step1  = Step(setup1, InFlight, hasBreakpoint = false)
      val step2  = Step(setup2, Pending, hasBreakpoint = false)

      val stepList = StepList(List(step1, step2))

      stepList.nextPending.value should ===(step2)
    }

    "return none when no pending step present" in {
      val setup1 = Setup(Prefix("esw.ocs.move1"), CommandName("test1"), None)
      val setup2 = Setup(Prefix("esw.ocs.move2"), CommandName("test2"), None)
      val step1  = models.Step(setup1, Finished.Success, hasBreakpoint = false)
      val step2  = Step(setup2, InFlight, hasBreakpoint = false)

      val stepList = StepList(List(step1, step2))

      stepList.nextPending should ===(None)
    }
  }

  "nextExecutable" must {
    val setup1 = Setup(Prefix("esw.ocs.move1"), CommandName("test1"), None)
    val setup2 = Setup(Prefix("esw.ocs.move2"), CommandName("test2"), None)

    "return step when next step exists and is not paused" in {
      val step1 = Step(setup1, InFlight, hasBreakpoint = false)
      val step2 = Step(setup2, Pending, hasBreakpoint = false)

      val stepList = StepList(List(step1, step2))
      stepList.nextExecutable.value should ===(step2)
    }
    "return none when next step doesn't exist" in {
      val step1 = Step(setup1, InFlight, hasBreakpoint = false)

      val stepList = StepList(List(step1))
      stepList.nextExecutable should ===(None)
    }
    "return none when next step is paused" in {
      val step1 = Step(setup1, InFlight, hasBreakpoint = false)
      val step2 = Step(setup2, Pending, hasBreakpoint = true)

      val stepList = StepList(List(step1, step2))
      stepList.nextExecutable should ===(None)
    }
  }

  "replace" must {
    val setup1 = Setup(Prefix("esw.ocs.move1"), CommandName("test1"), None)
    val setup2 = Setup(Prefix("esw.ocs.move2"), CommandName("test2"), None)
    val setup3 = Setup(Prefix("esw.ocs.move3"), CommandName("test3"), None)
    val setup4 = Setup(Prefix("esw.ocs.move4"), CommandName("test4"), None)
    val setup5 = Setup(Prefix("esw.ocs.move5"), CommandName("test5"), None)

    "replace step with given list of steps when Id matches and is in Pending status | ESW-108" in {
      val step1 = Step(setup1, InFlight, hasBreakpoint = false)
      val step2 = Step(setup2, Pending, hasBreakpoint = false)
      val step3 = Step(setup3, Pending, hasBreakpoint = false)

      val stepList        = StepList(List(step1, step2, step3))
      val updatedStepList = stepList.replace(step2.id, List(setup4, setup5))

      val setup4Id = updatedStepList.map(_.steps(1).id).toOption.get
      val setup5Id = updatedStepList.map(_.steps(2).id).toOption.get

      updatedStepList.toOption.get should ===(
        StepList(List(step1, Step(setup4).withId(setup4Id), Step(setup5).withId(setup5Id), step3))
      )
    }

    "fail with CannotOperateOnAnInFlightOrFinishedStep error when Id matches but is in InFlight status | ESW-108" in {
      val step1Status = Finished.Success
      val step2Status = InFlight
      val step1       = models.Step(setup1, step1Status, hasBreakpoint = false)
      val step2       = Step(setup2, step2Status, hasBreakpoint = false)
      val step3       = Step(setup3, Pending, hasBreakpoint = false)

      val stepList        = StepList(List(step1, step2, step3))
      val updatedStepList = stepList.replace(step2.id, List(setup4, setup5))
      updatedStepList.left.value should ===(CannotOperateOnAnInFlightOrFinishedStep)
    }

    "fail with CannotOperateOnAnInFlightOrFinishedStep error when Id matches but is in Finished status | ESW-108" in {
      val step1Status = Finished.Success
      val step2Status = Finished.Success
      val step1       = models.Step(setup1, step1Status, hasBreakpoint = false)
      val step2       = models.Step(setup2, step2Status, hasBreakpoint = false)
      val step3       = Step(setup3, Pending, hasBreakpoint = false)

      val stepList        = StepList(List(step1, step2, step3))
      val id              = step2.id
      val updatedStepList = stepList.replace(id, List(setup4, setup5))
      updatedStepList.left.value should ===(CannotOperateOnAnInFlightOrFinishedStep)
    }

    "fail with IdDoesNotExist error when provided Id does't exist in StepList | ESW-108" in {
      val step1 = Step(setup1, InFlight, hasBreakpoint = false)
      val step2 = Step(setup2, Pending, hasBreakpoint = false)

      val stepList = StepList(List(step1, step2))

      val invalidId       = Id()
      val updatedStepList = stepList.replace(invalidId, List(setup4, setup5))
      updatedStepList.left.value should ===(IdDoesNotExist(invalidId))
    }
  }

  "prepend" must {
    val setup1 = Setup(Prefix("esw.ocs.move1"), CommandName("test1"), None)
    val setup2 = Setup(Prefix("esw.ocs.move2"), CommandName("test2"), None)
    val setup3 = Setup(Prefix("esw.ocs.move3"), CommandName("test3"), None)
    val setup4 = Setup(Prefix("esw.ocs.move4"), CommandName("test4"), None)

    "add provided steps before next pending step | ESW-113" in {
      val step1 = Step(setup1, InFlight, hasBreakpoint = false)
      val step2 = Step(setup2, Pending, hasBreakpoint = false)

      val stepList        = StepList(List(step1, step2))
      val updatedStepList = stepList.prepend(List(setup3, setup4))
      val setup3Id        = updatedStepList.steps(1).id
      val setup4Id        = updatedStepList.steps(2).id
      updatedStepList should ===(StepList(List(step1, Step(setup3).withId(setup3Id), Step(setup4).withId(setup4Id), step2)))
    }

    "add provided steps at the end of StepList when StepList doesn't have Pending step | ESW-113" in {
      val step1 = Step(setup1, InFlight, hasBreakpoint = false)
      val step2 = Step(setup2, InFlight, hasBreakpoint = false)

      val stepList = StepList(List(step1, step2))

      val updatedStepList = stepList.prepend(List(setup3))
      val setup3Id        = updatedStepList.steps(2).id
      updatedStepList should ===(StepList(List(step1, step2, Step(setup3).withId(setup3Id))))
    }
  }

  "append" must {
    val setup1 = Setup(Prefix("esw.ocs.move1"), CommandName("test1"), None)
    val setup2 = Setup(Prefix("esw.ocs.move2"), CommandName("test2"), None)
    val setup3 = Setup(Prefix("esw.ocs.move3"), CommandName("test3"), None)
    val setup4 = Setup(Prefix("esw.ocs.move4"), CommandName("test4"), None)

    "add provided steps at the end of StepList | ESW-114" in {
      val step1 = Step(setup1, InFlight, hasBreakpoint = false)
      val step2 = Step(setup2, Pending, hasBreakpoint = false)

      val stepList        = StepList(List(step1, step2))
      val updatedStepList = stepList.append(List(setup3, setup4))
      val setup3Id        = updatedStepList.steps(2).id
      val setup4Id        = updatedStepList.steps(3).id
      updatedStepList should ===(StepList(List(step1, step2, Step(setup3).withId(setup3Id), Step(setup4).withId(setup4Id))))
    }
  }

  "delete" must {
    val setup1 = Setup(Prefix("esw.ocs.move1"), CommandName("test1"), None)
    val setup2 = Setup(Prefix("esw.ocs.move2"), CommandName("test2"), None)
    val setup3 = Setup(Prefix("esw.ocs.move3"), CommandName("test3"), None)

    "delete provided id when step status is Pending | ESW-112" in {
      val step1 = models.Step(setup1, Finished.Success, hasBreakpoint = false)
      val step2 = Step(setup2, Pending, hasBreakpoint = false)
      val step3 = Step(setup3, Pending, hasBreakpoint = false)

      val stepList        = StepList(List(step1, step2, step3))
      val updatedStepList = stepList.delete(step2.id)
      updatedStepList.toOption.get should ===(StepList(List(step1, step3)))
    }

    "fail with CannotOperateOnAnInFlightOrFinishedStep error when step status is inFlight | ESW-112" in {
      val step1 = Step(setup1, InFlight, hasBreakpoint = false)
      val step2 = Step(setup2, Pending, hasBreakpoint = false)

      val stepList        = StepList(List(step1, step2))
      val updatedStepList = stepList.delete(step1.id)
      updatedStepList.left.value should ===(CannotOperateOnAnInFlightOrFinishedStep)
    }

    "fail with CannotOperateOnAnInFlightOrFinishedStep error when step status is Finished | ESW-112" in {
      val step1Status = Finished.Success
      val step1       = models.Step(setup1, step1Status, hasBreakpoint = false)
      val step2       = Step(setup2, InFlight, hasBreakpoint = false)
      val step3       = Step(setup3, Pending, hasBreakpoint = false)

      val stepList        = StepList(List(step1, step2, step3))
      val updatedStepList = stepList.delete(step1.id)
      updatedStepList.left.value should ===(CannotOperateOnAnInFlightOrFinishedStep)
    }
    "fail with IdDoesNotExist error when step does not exist | ESW-112" in {
      val step1 = Step(setup1, InFlight, hasBreakpoint = false)

      val stepList        = StepList(List(step1))
      val invalidId       = Id()
      val updatedStepList = stepList.delete(invalidId)
      updatedStepList.left.value should ===(IdDoesNotExist(invalidId))
    }
  }

  "insertAfter" must {
    val setup1 = Setup(Prefix("esw.ocs.move1"), CommandName("test1"), None)
    val setup2 = Setup(Prefix("esw.ocs.move2"), CommandName("test2"), None)
    val setup3 = Setup(Prefix("esw.ocs.move3"), CommandName("test3"), None)
    val setup4 = Setup(Prefix("esw.ocs.move4"), CommandName("test4"), None)

    "insert provided commands after given Id | ESW-111" in {
      val step1 = models.Step(setup1, Finished.Success, hasBreakpoint = false)
      val step2 = Step(setup2, Pending, hasBreakpoint = false)

      val stepList        = StepList(List(step1, step2))
      val updatedStepList = stepList.insertAfter(step1.id, List(setup3, setup4))
      val setup3Id        = updatedStepList.map(_.steps(1).id).toOption.get
      val setup4Id        = updatedStepList.map(_.steps(2).id).toOption.get
      updatedStepList.toOption.get should ===(
        StepList(List(step1, Step(setup3).withId(setup3Id), Step(setup4).withId(setup4Id), step2))
      )
    }

    "insert provided commands after last InFlight step | ESW-111" in {
      val step1 = Step(setup1, InFlight, hasBreakpoint = false)

      val stepList        = StepList(List(step1))
      val updatedStepList = stepList.insertAfter(step1.id, List(setup2))
      val setup2Id        = updatedStepList.map(_.steps(1).id).toOption.get
      updatedStepList.toOption.get should ===(StepList(List(step1, Step(setup2).withId(setup2Id))))
    }

    "fail with IdDoesNotExist error when provided Id doesn't exist in StepList | ESW-111" in {
      val step1 = Step(setup1, InFlight, hasBreakpoint = false)
      val step2 = Step(setup2, Pending, hasBreakpoint = false)

      val stepList = StepList(List(step1, step2))

      val invalidId       = Id()
      val updatedStepList = stepList.insertAfter(invalidId, List(setup3))
      updatedStepList.left.value should ===(IdDoesNotExist(invalidId))
    }

    "fail with CannotOperateOnAnInFlightOrFinishedStep error when trying to insert before a Finished step in StepList | ESW-111" in {
      val step1 = models.Step(setup1, Finished.Success, hasBreakpoint = false)
      val step2 = models.Step(setup2, Finished.Success, hasBreakpoint = false)
      val step3 = Step(setup3, InFlight, hasBreakpoint = false)

      val stepList = StepList(List(step1, step2, step3))

      val updatedStepList = stepList.insertAfter(step1.id, List(setup4))
      updatedStepList.left.value should ===(CannotOperateOnAnInFlightOrFinishedStep)
    }

    "fail with CannotOperateOnAnInFlightOrFinishedStep error when trying to insert before an inFlight step in StepList | ESW-111" in {
      val step1 = models.Step(setup1, Finished.Success, hasBreakpoint = false)
      val step2 = Step(setup2, InFlight, hasBreakpoint = false)

      val stepList = StepList(List(step1, step2))

      val updatedStepList = stepList.insertAfter(step1.id, List(setup3))
      updatedStepList.left.value should ===(CannotOperateOnAnInFlightOrFinishedStep)
    }
  }

  "discardPending" must {
    val setup1 = Setup(Prefix("esw.ocs.move1"), CommandName("test1"), None)
    val setup2 = Setup(Prefix("esw.ocs.move2"), CommandName("test2"), None)
    val setup3 = Setup(Prefix("esw.ocs.move3"), CommandName("test3"), None)
    val setup4 = Setup(Prefix("esw.ocs.move4"), CommandName("test4"), None)

    "discard all the pending steps from StepList | ESW-110" in {
      val step1 = models.Step(setup1, Finished.Success, hasBreakpoint = false)
      val step2 = Step(setup2, Pending, hasBreakpoint = false)
      val step3 = Step(setup3, InFlight, hasBreakpoint = false)
      val step4 = Step(setup4, Pending, hasBreakpoint = false)

      val stepList        = StepList(List(step1, step2, step3, step4))
      val updatedStepList = stepList.discardPending
      updatedStepList should ===(StepList(List(step1, step3)))
    }
  }

  "addBreakpoints" must {
    val setup1 = Setup(Prefix("esw.ocs.move1"), CommandName("test1"), None)
    val setup2 = Setup(Prefix("esw.ocs.move2"), CommandName("test2"), None)

    "add breakpoint to provided id when step status is Pending | ESW-106" in {
      val step1 = models.Step(setup1, Finished.Success, hasBreakpoint = false)
      val step2 = Step(setup2, Pending, hasBreakpoint = false)

      val stepList        = StepList(List(step1, step2))
      val updatedStepList = stepList.addBreakpoint(step2.id)
      updatedStepList.toOption.get should ===(StepList(List(step1, step2.copy(hasBreakpoint = true))))
    }

    "fail with IdDoesNotExist error when provided id does not exist | ESW-106" in {
      val step1 = Step(setup1, Pending, hasBreakpoint = false)

      val stepList        = StepList(List(step1))
      val invalidId       = Id()
      val updatedStepList = stepList.addBreakpoint(invalidId)
      updatedStepList.left.value shouldBe IdDoesNotExist(invalidId)
    }
  }

  "removeBreakpoints" must {
    val setup1 = Setup(Prefix("esw.ocs.move1"), CommandName("test1"), None)
    val setup2 = Setup(Prefix("esw.ocs.move2"), CommandName("test2"), None)

    "remove breakpoint from provided id | ESW-107" in {
      val step1 = models.Step(setup1, Finished.Success, hasBreakpoint = false)
      val step2 = Step(setup2, Pending, hasBreakpoint = true)

      val stepList        = StepList(List(step1, step2))
      val updatedStepList = stepList.removeBreakpoint(step2.id)
      updatedStepList.toOption.get should ===(StepList(List(step1, step2.copy(hasBreakpoint = false))))
    }

    "fail with IdDoesNotExist error when provided id does not exist | ESW-107" in {
      val step1 = Step(setup1, Pending, hasBreakpoint = false)

      val stepList        = StepList(List(step1))
      val invalidId       = Id()
      val updatedStepList = stepList.removeBreakpoint(invalidId)
      updatedStepList.left.value shouldBe IdDoesNotExist(invalidId)
    }
  }

  "pause" must {
    val setup1 = Setup(Prefix("esw.ocs.move1"), CommandName("test1"), None)
    val setup2 = Setup(Prefix("esw.ocs.move2"), CommandName("test2"), None)
    val setup3 = Setup(Prefix("esw.ocs.move3"), CommandName("test3"), None)
    val setup4 = Setup(Prefix("esw.ocs.move4"), CommandName("test4"), None)

    "add breakpoint to next pending step | ESW-104" in {
      val step1 = models.Step(setup1, Finished.Success, hasBreakpoint = false)
      val step2 = Step(setup2, InFlight, hasBreakpoint = false)
      val step3 = Step(setup3, Pending, hasBreakpoint = false)
      val step4 = Step(setup4, Pending, hasBreakpoint = false)

      val stepList        = StepList(List(step1, step2, step3, step4))
      val updatedStepList = stepList.pause
      updatedStepList.toOption.get should ===(StepList(List(step1, step2, step3.copy(hasBreakpoint = true), step4)))
    }

    "fail with CannotOperateOnAnInFlightOrFinishedStep error when Pending step doesn't exist in StepList | ESW-104" in {
      val step1 = models.Step(setup1, Finished.Success, hasBreakpoint = false)
      val step2 = Step(setup2, InFlight, hasBreakpoint = false)

      val stepList        = StepList(List(step1, step2))
      val updatedStepList = stepList.pause
      updatedStepList.left.value should ===(CannotOperateOnAnInFlightOrFinishedStep)
    }
  }

  "resume" must {
    val setup1 = Setup(Prefix("esw.ocs.move1"), CommandName("test1"), None)
    val setup2 = Setup(Prefix("esw.ocs.move2"), CommandName("test2"), None)
    val setup3 = Setup(Prefix("esw.ocs.move3"), CommandName("test3"), None)

    "remove breakpoint from next pending step | ESW-105" in {
      val step1 = models.Step(setup1, Finished.Success, hasBreakpoint = false)
      val step2 = Step(setup2, InFlight, hasBreakpoint = false)
      val step3 = Step(setup3, Pending, hasBreakpoint = true)

      val stepList        = StepList(List(step1, step2, step3))
      val updatedStepList = stepList.resume
      updatedStepList shouldBe StepList(List(step1, step2, step3.copy(hasBreakpoint = false)))
    }

    "be no-op when Pending step doesn't exist in StepList | ESW-105" in {
      val step1 = models.Step(setup1, Finished.Success, hasBreakpoint = false)
      val step2 = Step(setup2, InFlight, hasBreakpoint = false)

      val stepList        = StepList(List(step1, step2))
      val updatedStepList = stepList.resume
      updatedStepList should ===(stepList)
    }
  }
}
