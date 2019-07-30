package esw.ocs.api.models

import csw.command.client.messages.sequencer.SequenceError.DuplicateIdsFound
import csw.params.commands.CommandResponse.Completed
import csw.params.commands._
import csw.params.core.models.{Id, Prefix}
import esw.ocs.api.models.StepStatus.{Finished, InFlight, Pending}
import esw.ocs.api.models.messages.error.StepListError._
import esw.ocs.api.{BaseTestSuite, models}

class StepListTest extends BaseTestSuite {
  def finished(id: Id): Finished.Success = Finished.Success(Completed(id))

  "empty" must {
    "create empty StepList" in {
      StepList.empty.steps should ===(Nil)
    }
  }

  "apply" must {
    "return a StepList" in {
      val setup1 = Setup(Prefix("ocs.move1"), CommandName("test1"), None)
      val setup2 = Setup(Prefix("ocs.move2"), CommandName("test2"), None)

      val stepList = StepList(Sequence(setup1, setup2)).toOption.get

      stepList.steps.length should ===(2)
    }

    "fail when duplicate Ids provided" in {
      val setup1 = Setup(Prefix("ocs.move1"), CommandName("test1"), None)
      val setup2 = Setup(Prefix("ocs.move2"), CommandName("test2"), None).copy(runId = setup1.runId)

      val sequence = Sequence(setup1, setup2)
      StepList(sequence).left.value should ===(DuplicateIdsFound)
    }
  }

  "isFinished" must {
    val setup1 = Setup(Prefix("ocs.move1"), CommandName("test1"), None)
    val setup2 = Setup(Prefix("ocs.move2"), CommandName("test2"), None)

    "return false when StepList is empty" in {
      val stepList = StepList(Id(), Nil)
      stepList.isFinished should ===(false)
    }

    "return true when all steps are Finished" in {
      val step1 = Step(setup1, finished(setup1.runId), hasBreakpoint = false)
      val step2 = models.Step(setup2, finished(setup2.runId), hasBreakpoint = false)

      val stepList = StepList(Id(), List(step1, step2))
      stepList.isFinished should ===(true)
    }

    "return true when any step is Failed" in {
      val step1 = Step(setup1, Finished.Failure(Completed(setup1.runId)), hasBreakpoint = false)
      val step2 = Step(setup2, InFlight, hasBreakpoint = false)

      val stepList = StepList(Id(), List(step1, step2))
      stepList.isFinished should ===(true)
    }

    "return false when any step is not Finished" in {
      val step1 = models.Step(setup1, finished(setup1.runId), hasBreakpoint = false)
      val step2 = Step(setup2, InFlight, hasBreakpoint = false)

      val stepList = StepList(Id(), List(step1, step2))
      stepList.isFinished should ===(false)
    }
  }

  "isPaused" must {
    val setup1 = Setup(Prefix("ocs.move1"), CommandName("test1"), None)
    val setup2 = Setup(Prefix("ocs.move2"), CommandName("test2"), None)

    "return true when next step exists and it has a breakpoint" in {
      val step1 = Step(setup1, InFlight, hasBreakpoint = false)
      val step2 = Step(setup2, Pending, hasBreakpoint = true)

      val stepList = StepList(Id(), List(step1, step2))
      stepList.isPaused should ===(true)
    }

    "return false when next step exists but doesn't have breakpoint" in {
      val step1 = Step(setup1, InFlight, hasBreakpoint = false)
      val step2 = Step(setup2, Pending, hasBreakpoint = false)

      val stepList = StepList(Id(), List(step1, step2))
      stepList.isPaused should ===(false)
    }

    "return false when next step doesn't exist" in {
      val step1 = Step(setup1, InFlight, hasBreakpoint = false)

      val stepList = StepList(Id(), List(step1))
      stepList.isPaused should ===(false)
    }
  }

  "isInFlight" must {
    val setup1 = Setup(Prefix("ocs.move1"), CommandName("test1"), None)
    val setup2 = Setup(Prefix("ocs.move2"), CommandName("test2"), None)

    "return true when at least one InFlight step exist" in {
      val step1 = Step(setup1, InFlight, hasBreakpoint = false)
      val step2 = Step(setup2, Pending, hasBreakpoint = true)

      val stepList = StepList(Id(), List(step1, step2))
      stepList.isInFlight should ===(true)
    }

    "return false when no InFlight step exist" in {
      val step1 = models.Step(setup1, finished(setup1.runId), hasBreakpoint = false)
      val step2 = Step(setup2, Pending, hasBreakpoint = false)

      val stepList = StepList(Id(), List(step1, step2))
      stepList.isInFlight should ===(false)
    }
  }

  "nextPending" must {
    "return next pending step" in {
      val setup1 = Setup(Prefix("ocs.move1"), CommandName("test1"), None)
      val setup2 = Setup(Prefix("ocs.move2"), CommandName("test2"), None)
      val step1  = Step(setup1, InFlight, hasBreakpoint = false)
      val step2  = Step(setup2, Pending, hasBreakpoint = false)

      val stepList = StepList(Id(), List(step1, step2))

      stepList.nextPending.value should ===(step2)
    }

    "return none when no pending step present" in {
      val setup1 = Setup(Prefix("ocs.move1"), CommandName("test1"), None)
      val setup2 = Setup(Prefix("ocs.move2"), CommandName("test2"), None)
      val step1  = models.Step(setup1, finished(setup1.runId), hasBreakpoint = false)
      val step2  = Step(setup2, InFlight, hasBreakpoint = false)

      val stepList = StepList(Id(), List(step1, step2))

      stepList.nextPending should ===(None)
    }
  }

  "nextExecutable" must {
    val setup1 = Setup(Prefix("ocs.move1"), CommandName("test1"), None)
    val setup2 = Setup(Prefix("ocs.move2"), CommandName("test2"), None)

    "return step when next step exists and is not paused" in {
      val step1 = Step(setup1, InFlight, hasBreakpoint = false)
      val step2 = Step(setup2, Pending, hasBreakpoint = false)

      val stepList = StepList(Id(), List(step1, step2))
      stepList.nextExecutable.value should ===(step2)
    }
    "return none when next step doesn't exist" in {
      val step1 = Step(setup1, InFlight, hasBreakpoint = false)

      val stepList = StepList(Id(), List(step1))
      stepList.nextExecutable should ===(None)
    }
    "return none when next step is paused" in {
      val step1 = Step(setup1, InFlight, hasBreakpoint = false)
      val step2 = Step(setup2, Pending, hasBreakpoint = true)

      val stepList = StepList(Id(), List(step1, step2))
      stepList.nextExecutable should ===(None)
    }
  }

  "replace" must {
    val setup1 = Setup(Prefix("ocs.move1"), CommandName("test1"), None)
    val setup2 = Setup(Prefix("ocs.move2"), CommandName("test2"), None)
    val setup3 = Setup(Prefix("ocs.move3"), CommandName("test3"), None)
    val setup4 = Setup(Prefix("ocs.move4"), CommandName("test4"), None)
    val setup5 = Setup(Prefix("ocs.move5"), CommandName("test5"), None)

    "replace step with given list of steps when Id matches and is in Pending status | ESW-108" in {
      val step1 = Step(setup1, InFlight, hasBreakpoint = false)
      val step2 = Step(setup2, Pending, hasBreakpoint = false)
      val step3 = Step(setup3, Pending, hasBreakpoint = false)

      val id              = Id()
      val stepList        = StepList(id, List(step1, step2, step3))
      val updatedStepList = stepList.replace(setup2.runId, List(setup4, setup5))
      updatedStepList.toOption.get should ===(StepList(id, List(step1, Step(setup4), Step(setup5), step3)))
    }

    "fail with NotSupported error when Id matches but is in InFlight status | ESW-108" in {
      val step1Status = finished(setup1.runId)
      val step2Status = InFlight
      val step1       = models.Step(setup1, step1Status, hasBreakpoint = false)
      val step2       = Step(setup2, step2Status, hasBreakpoint = false)
      val step3       = Step(setup3, Pending, hasBreakpoint = false)

      val stepList        = StepList(Id(), List(step1, step2, step3))
      val id              = setup2.runId
      val updatedStepList = stepList.replace(id, List(setup4, setup5))
      updatedStepList.left.value should ===(NotSupported(step2Status))
    }

    "fail with NotSupported error when Id matches but is in Finished status | ESW-108" in {
      val step1Status = finished(setup1.runId)
      val step2Status = finished(setup2.runId)
      val step1       = models.Step(setup1, step1Status, hasBreakpoint = false)
      val step2       = models.Step(setup2, step2Status, hasBreakpoint = false)
      val step3       = Step(setup3, Pending, hasBreakpoint = false)

      val stepList        = StepList(Id(), List(step1, step2, step3))
      val id              = setup2.runId
      val updatedStepList = stepList.replace(id, List(setup4, setup5))
      updatedStepList.left.value should ===(NotSupported(step2Status))
    }

    "fail with IdDoesNotExist error when provided Id does't exist in StepList | ESW-108" in {
      val step1 = Step(setup1, InFlight, hasBreakpoint = false)
      val step2 = Step(setup2, Pending, hasBreakpoint = false)

      val stepList = StepList(Id(), List(step1, step2))

      val invalidId       = Id()
      val updatedStepList = stepList.replace(invalidId, List(setup4, setup5))
      updatedStepList.left.value should ===(IdDoesNotExist(invalidId))
    }

    "fail with NotAllowedOnFinishedSeq error when StepList is finished | ESW-108" in {
      val step1 = models.Step(setup1, finished(setup1.runId), hasBreakpoint = false)
      val step2 = models.Step(setup2, finished(setup2.runId), hasBreakpoint = false)

      val id              = Id()
      val stepList        = StepList(id, List(step1, step2))
      val updatedStepList = stepList.replace(setup2.runId, List(setup4, setup5))
      updatedStepList.left.value should ===(NotAllowedOnFinishedSeq)
    }
  }

  "prepend" must {
    val setup1 = Setup(Prefix("ocs.move1"), CommandName("test1"), None)
    val setup2 = Setup(Prefix("ocs.move2"), CommandName("test2"), None)
    val setup3 = Setup(Prefix("ocs.move3"), CommandName("test3"), None)
    val setup4 = Setup(Prefix("ocs.move4"), CommandName("test4"), None)

    "add provided steps before next pending step | ESW-113" in {
      val step1 = Step(setup1, InFlight, hasBreakpoint = false)
      val step2 = Step(setup2, Pending, hasBreakpoint = false)

      val id              = Id()
      val stepList        = StepList(id, List(step1, step2))
      val updatedStepList = stepList.prepend(List(setup3, setup4))
      updatedStepList.toOption.get should ===(StepList(id, List(step1, Step(setup3), Step(setup4), step2)))
    }

    "add provided steps at the end of StepList when StepList doesn't have Pending step | ESW-113" in {
      val step1 = Step(setup1, InFlight, hasBreakpoint = false)
      val step2 = Step(setup2, InFlight, hasBreakpoint = false)

      val stepList = StepList(Id(), List(step1, step2))

      val updatedStepList = stepList.prepend(List(setup3))
      updatedStepList.toOption.get should ===(StepList(stepList.runId, List(step1, step2, Step(setup3))))
    }

    "fail with NotAllowedOnFinishedSeq error when StepList is finished | ESW-113" in {
      val step1 = models.Step(setup1, finished(setup1.runId), hasBreakpoint = false)
      val step2 = models.Step(setup2, finished(setup2.runId), hasBreakpoint = false)

      val stepList        = StepList(Id(), List(step1, step2))
      val updatedStepList = stepList.prepend(List(setup3))
      updatedStepList.left.value should ===(NotAllowedOnFinishedSeq)
    }

  }

  "append" must {
    val setup1 = Setup(Prefix("ocs.move1"), CommandName("test1"), None)
    val setup2 = Setup(Prefix("ocs.move2"), CommandName("test2"), None)
    val setup3 = Setup(Prefix("ocs.move3"), CommandName("test3"), None)
    val setup4 = Setup(Prefix("ocs.move4"), CommandName("test4"), None)

    "add provided steps at the end of StepList | ESW-114" in {
      val step1 = Step(setup1, InFlight, hasBreakpoint = false)
      val step2 = Step(setup2, Pending, hasBreakpoint = false)

      val id              = Id()
      val stepList        = StepList(id, List(step1, step2))
      val updatedStepList = stepList.append(List(setup3, setup4))
      updatedStepList.toOption.get should ===(StepList(id, List(step1, step2, Step(setup3), Step(setup4))))
    }

    "fail with NotAllowedOnFinishedSeq error when StepList is finished | ESW-114" in {
      val step1 = models.Step(setup1, finished(setup1.runId), hasBreakpoint = false)
      val step2 = models.Step(setup2, finished(setup2.runId), hasBreakpoint = false)

      val stepList        = StepList(Id(), List(step1, step2))
      val updatedStepList = stepList.append(List(setup3))
      updatedStepList.left.value should ===(NotAllowedOnFinishedSeq)
    }
  }

  "delete" must {
    val setup1 = Setup(Prefix("ocs.move1"), CommandName("test1"), None)
    val setup2 = Setup(Prefix("ocs.move2"), CommandName("test2"), None)
    val setup3 = Setup(Prefix("ocs.move3"), CommandName("test3"), None)

    "delete provided id when step status is Pending | ESW-112" in {
      val step1 = models.Step(setup1, finished(setup1.runId), hasBreakpoint = false)
      val step2 = Step(setup2, Pending, hasBreakpoint = false)
      val step3 = Step(setup3, Pending, hasBreakpoint = false)

      val id              = Id()
      val stepList        = StepList(id, List(step1, step2, step3))
      val updatedStepList = stepList.delete(setup2.runId)
      updatedStepList.toOption.get should ===(StepList(id, List(step1, step3)))
    }

    "fail with DeleteNotSupported error when step status is other than Pending | ESW-112" in {
      val step1 = Step(setup1, InFlight, hasBreakpoint = false)
      val step2 = Step(setup2, Pending, hasBreakpoint = false)

      val stepList        = StepList(Id(), List(step1, step2))
      val updatedStepList = stepList.delete(setup1.runId)
      updatedStepList.left.value should ===(NotSupported(InFlight))
    }

    "fail with IdDoesNotExist error when step does not exist | ESW-112" in {
      val step1 = Step(setup1, InFlight, hasBreakpoint = false)

      val stepList        = StepList(Id(), List(step1))
      val invalidId       = Id()
      val updatedStepList = stepList.delete(invalidId)
      updatedStepList.left.value should ===(IdDoesNotExist(invalidId))
    }

    "fail with NotAllowedOnFinishedSeq error when StepList is finished | ESW-112" in {
      val step1 = models.Step(setup1, finished(setup1.runId), hasBreakpoint = false)
      val step2 = models.Step(setup2, finished(setup2.runId), hasBreakpoint = false)

      val id              = Id()
      val stepList        = StepList(id, List(step1, step2))
      val updatedStepList = stepList.delete(setup1.runId)
      updatedStepList.left.value should ===(NotAllowedOnFinishedSeq)
    }
  }

  "insertAfter" must {
    val setup1 = Setup(Prefix("ocs.move1"), CommandName("test1"), None)
    val setup2 = Setup(Prefix("ocs.move2"), CommandName("test2"), None)
    val setup3 = Setup(Prefix("ocs.move3"), CommandName("test3"), None)
    val setup4 = Setup(Prefix("ocs.move4"), CommandName("test4"), None)

    "insert provided commands after given Id | ESW-111" in {
      val step1 = models.Step(setup1, finished(setup1.runId), hasBreakpoint = false)
      val step2 = Step(setup2, Pending, hasBreakpoint = false)

      val id              = Id()
      val stepList        = StepList(id, List(step1, step2))
      val updatedStepList = stepList.insertAfter(step1.id, List(setup3, setup4))
      updatedStepList.toOption.get should ===(StepList(id, List(step1, Step(setup3), Step(setup4), step2)))
    }

    "insert provided commands after last InFlight step | ESW-111" in {
      val step1 = Step(setup1, InFlight, hasBreakpoint = false)

      val id              = Id()
      val stepList        = StepList(id, List(step1))
      val updatedStepList = stepList.insertAfter(step1.id, List(setup2))
      updatedStepList.toOption.get should ===(StepList(id, List(step1, Step(setup2))))
    }

    "fail with IdDoesNotExist error when provided Id does't exist in StepList | ESW-111" in {
      val step1 = Step(setup1, InFlight, hasBreakpoint = false)
      val step2 = Step(setup2, Pending, hasBreakpoint = false)

      val stepList = StepList(Id(), List(step1, step2))

      val invalidId       = Id()
      val updatedStepList = stepList.insertAfter(invalidId, List(setup3))
      updatedStepList.left.value should ===(IdDoesNotExist(invalidId))
    }

    "fail with NotSupported error when trying to insert before a Finished step in StepList | ESW-111" in {
      val step1 = models.Step(setup1, finished(setup1.runId), hasBreakpoint = false)
      val step2 = models.Step(setup2, finished(setup2.runId), hasBreakpoint = false)
      val step3 = Step(setup3, InFlight, hasBreakpoint = false)

      val stepList = StepList(Id(), List(step1, step2, step3))

      val updatedStepList = stepList.insertAfter(step1.id, List(setup4))
      updatedStepList.left.value should ===(NotSupported(finished(setup2.runId)))
    }

    "fail with NotSupported error when trying to insert before an inFlight step in StepList | ESW-111" in {
      val step1 = models.Step(setup1, finished(setup1.runId), hasBreakpoint = false)
      val step2 = Step(setup2, InFlight, hasBreakpoint = false)

      val stepList = StepList(Id(), List(step1, step2))

      val updatedStepList = stepList.insertAfter(step1.id, List(setup3))
      updatedStepList.left.value should ===(NotSupported(InFlight))
    }

    "fail with NotAllowedOnFinishedSeq error when StepList is finished | ESW-111" in {
      val step1 = models.Step(setup1, finished(setup1.runId), hasBreakpoint = false)
      val step2 = models.Step(setup2, finished(setup2.runId), hasBreakpoint = false)

      val id              = Id()
      val stepList        = StepList(id, List(step1, step2))
      val updatedStepList = stepList.insertAfter(step1.id, List(setup3, setup4))
      updatedStepList.left.value should ===(NotAllowedOnFinishedSeq)
    }
  }

  "discardPending" must {
    val setup1 = Setup(Prefix("ocs.move1"), CommandName("test1"), None)
    val setup2 = Setup(Prefix("ocs.move2"), CommandName("test2"), None)
    val setup3 = Setup(Prefix("ocs.move3"), CommandName("test3"), None)
    val setup4 = Setup(Prefix("ocs.move4"), CommandName("test4"), None)

    "discard all the pending steps from StepList | ESW-110" in {
      val step1 = models.Step(setup1, finished(setup1.runId), hasBreakpoint = false)
      val step2 = Step(setup2, Pending, hasBreakpoint = false)
      val step3 = Step(setup3, InFlight, hasBreakpoint = false)
      val step4 = Step(setup4, Pending, hasBreakpoint = false)

      val id              = Id()
      val stepList        = StepList(id, List(step1, step2, step3, step4))
      val updatedStepList = stepList.discardPending
      updatedStepList.toOption.get should ===(StepList(id, List(step1, step3)))
    }

    "fail with NotAllowedOnFinishedSeq error when StepList is finished | ESW-110" in {
      val step1 = models.Step(setup1, finished(setup1.runId), hasBreakpoint = false)
      val step2 = models.Step(setup2, finished(setup2.runId), hasBreakpoint = false)

      val id              = Id()
      val stepList        = StepList(id, List(step1, step2))
      val updatedStepList = stepList.discardPending
      updatedStepList.left.value should ===(NotAllowedOnFinishedSeq)
    }
  }

  "addBreakpoints" must {
    val setup1 = Setup(Prefix("ocs.move1"), CommandName("test1"), None)
    val setup2 = Setup(Prefix("ocs.move2"), CommandName("test2"), None)

    "add breakpoint to provided id when step status is Pending | ESW-106" in {
      val step1 = models.Step(setup1, finished(setup1.runId), hasBreakpoint = false)
      val step2 = Step(setup2, Pending, hasBreakpoint = false)

      val id              = Id()
      val stepList        = StepList(id, List(step1, step2))
      val updatedStepList = stepList.addBreakpoint(setup2.runId)
      updatedStepList.toOption.get should ===(StepList(id, List(step1, step2.copy(hasBreakpoint = true))))
    }

    "fail with IdDoesNotExist error when provided id does not exist | ESW-106" in {
      val step1 = Step(setup1, Pending, hasBreakpoint = false)

      val stepList        = StepList(Id(), List(step1))
      val invalidId       = Id()
      val updatedStepList = stepList.addBreakpoint(invalidId)
      updatedStepList.left.value shouldBe IdDoesNotExist(invalidId)
    }

    "fail with NotAllowedOnFinishedSeq error when StepList is finished | ESW-106" in {
      val step1 = models.Step(setup1, finished(setup1.runId), hasBreakpoint = false)
      val step2 = models.Step(setup2, finished(setup2.runId), hasBreakpoint = false)

      val id              = Id()
      val stepList        = StepList(id, List(step1, step2))
      val updatedStepList = stepList.addBreakpoint(setup1.runId)
      updatedStepList.left.value should ===(NotAllowedOnFinishedSeq)
    }
  }

  "removeBreakpoints" must {
    val setup1 = Setup(Prefix("ocs.move1"), CommandName("test1"), None)
    val setup2 = Setup(Prefix("ocs.move2"), CommandName("test2"), None)

    "remove breakpoint from provided id | ESW-107" in {
      val step1 = models.Step(setup1, finished(setup1.runId), hasBreakpoint = false)
      val step2 = Step(setup2, Pending, hasBreakpoint = true)

      val id              = Id()
      val stepList        = StepList(id, List(step1, step2))
      val updatedStepList = stepList.removeBreakpoint(setup2.runId)
      updatedStepList.toOption.get should ===(StepList(id, List(step1, step2.copy(hasBreakpoint = false))))
    }

    "fail with IdDoesNotExist error when provided id does not exist | ESW-107" in {
      val step1 = Step(setup1, Pending, hasBreakpoint = false)

      val stepList        = StepList(Id(), List(step1))
      val invalidId       = Id()
      val updatedStepList = stepList.removeBreakpoint(invalidId)
      updatedStepList.left.value shouldBe IdDoesNotExist(invalidId)
    }

    "fail with NotAllowedOnFinishedSeq error when StepList is finished | ESW-107" in {
      val step1 = models.Step(setup1, finished(setup1.runId), hasBreakpoint = true)
      val step2 = models.Step(setup2, finished(setup2.runId), hasBreakpoint = false)

      val id              = Id()
      val stepList        = StepList(id, List(step1, step2))
      val updatedStepList = stepList.removeBreakpoint(setup1.runId)
      updatedStepList.left.value should ===(NotAllowedOnFinishedSeq)
    }
  }

  "pause" must {
    val setup1 = Setup(Prefix("ocs.move1"), CommandName("test1"), None)
    val setup2 = Setup(Prefix("ocs.move2"), CommandName("test2"), None)
    val setup3 = Setup(Prefix("ocs.move3"), CommandName("test3"), None)
    val setup4 = Setup(Prefix("ocs.move4"), CommandName("test4"), None)

    "add breakpoint to next pending step | ESW-104" in {
      val step1 = models.Step(setup1, finished(setup1.runId), hasBreakpoint = false)
      val step2 = Step(setup2, InFlight, hasBreakpoint = false)
      val step3 = Step(setup3, Pending, hasBreakpoint = false)
      val step4 = Step(setup4, Pending, hasBreakpoint = false)

      val id              = Id()
      val stepList        = StepList(id, List(step1, step2, step3, step4))
      val updatedStepList = stepList.pause
      updatedStepList.toOption.get should ===(StepList(id, List(step1, step2, step3.copy(hasBreakpoint = true), step4)))
    }

    "fail with PauseFailed error when Pending step doesn't exist in StepList | ESW-104" in {
      val step1 = models.Step(setup1, finished(setup1.runId), hasBreakpoint = false)
      val step2 = Step(setup2, InFlight, hasBreakpoint = false)

      val stepList        = StepList(Id(), List(step1, step2))
      val updatedStepList = stepList.pause
      updatedStepList.left.value shouldBe PauseFailed
    }

    "fail with NotAllowedOnFinishedSeq error when StepList is finished | ESW-104" in {
      val step1 = models.Step(setup1, finished(setup1.runId), hasBreakpoint = true)
      val step2 = models.Step(setup2, finished(setup2.runId), hasBreakpoint = false)

      val stepList        = StepList(Id(), List(step1, step2))
      val updatedStepList = stepList.pause
      updatedStepList.left.value should ===(NotAllowedOnFinishedSeq)
    }
  }

  "resume" must {
    val setup1 = Setup(Prefix("ocs.move1"), CommandName("test1"), None)
    val setup2 = Setup(Prefix("ocs.move2"), CommandName("test2"), None)
    val setup3 = Setup(Prefix("ocs.move3"), CommandName("test3"), None)

    "remove breakpoint from next pending step | ESW-105" in {
      val step1 = models.Step(setup1, finished(setup1.runId), hasBreakpoint = false)
      val step2 = Step(setup2, InFlight, hasBreakpoint = false)
      val step3 = Step(setup3, Pending, hasBreakpoint = true)

      val id              = Id()
      val stepList        = StepList(id, List(step1, step2, step3))
      val updatedStepList = stepList.resume
      updatedStepList.toOption.get shouldBe StepList(id, List(step1, step2, step3.copy(hasBreakpoint = false)))
    }

    "be no-op when Pending step doesn't exist in StepList | ESW-105" in {
      val step1 = models.Step(setup1, finished(setup1.runId), hasBreakpoint = false)
      val step2 = Step(setup2, InFlight, hasBreakpoint = false)

      val stepList        = StepList(Id(), List(step1, step2))
      val updatedStepList = stepList.resume
      updatedStepList.toOption.get should ===(stepList)
    }

    "fail with NotAllowedOnFinishedSeq error when StepList is finished | ESW-105" in {
      val step1 = models.Step(setup1, finished(setup1.runId), hasBreakpoint = true)
      val step2 = models.Step(setup2, finished(setup2.runId), hasBreakpoint = false)

      val stepList        = StepList(Id(), List(step1, step2))
      val updatedStepList = stepList.resume
      updatedStepList.left.value should ===(NotAllowedOnFinishedSeq)
    }
  }

  "updateStatus" must {
    val setup1 = Setup(Prefix("ocs.move1"), CommandName("test1"), None)
    val setup2 = Setup(Prefix("ocs.move2"), CommandName("test2"), None)
    val setup3 = Setup(Prefix("ocs.move3"), CommandName("test3"), None)

    "update status of step matching provided Id with given status" in {
      val step1 = models.Step(setup1, finished(setup1.runId), hasBreakpoint = false)
      val step2 = Step(setup2, InFlight, hasBreakpoint = false)
      val step3 = Step(setup3, Pending, hasBreakpoint = false)

      val id               = Id()
      val stepList         = StepList(id, List(step1, step2, step3))
      val step2Status      = finished(setup2.runId)
      val updatedStepList1 = stepList.updateStatus(setup2.runId, step2Status)
      val updatedStep2     = step2.copy(status = step2Status)
      updatedStepList1.toOption.get shouldBe StepList(id, List(step1, updatedStep2, step3))

      val updatedStepList2 = updatedStepList1.toOption.get.updateStatus(setup3.runId, InFlight)
      val updatedStep3     = step3.copy(status = InFlight)
      updatedStepList2.toOption.get should ===(StepList(id, List(step1, updatedStep2, updatedStep3)))
    }

    "fail with UpdateFailed error when step status transition not allowed" in {
      val step1 = models.Step(setup1, finished(setup1.runId), hasBreakpoint = false)
      val step2 = Step(setup2, Pending, hasBreakpoint = false)

      val stepList        = StepList(Id(), List(step1, step2))
      val step2Status     = finished(setup2.runId)
      val updatedStepList = stepList.updateStatus(setup2.runId, step2Status)
      updatedStepList.left.value shouldBe UpdateNotSupported(Pending, step2Status)
    }

    "fail with IdDoesNotExist error when provided Id does't exist in StepList" in {
      val step1 = models.Step(setup1, finished(setup1.runId), hasBreakpoint = false)
      val step2 = Step(setup2, Pending, hasBreakpoint = false)

      val stepList        = StepList(Id(), List(step1, step2))
      val invalidId       = Id()
      val updatedStepList = stepList.updateStatus(invalidId, InFlight)

      updatedStepList.left.value shouldBe IdDoesNotExist(invalidId)
    }

    "fail with NotAllowedOnFinishedSeq error when StepList is finished" in {
      val step1Status = finished(setup1.runId)
      val step2Status = finished(setup2.runId)
      val step1       = models.Step(setup1, step1Status, hasBreakpoint = false)
      val step2       = models.Step(setup2, step2Status, hasBreakpoint = false)

      val stepList        = StepList(Id(), List(step1, step2))
      val updatedStepList = stepList.updateStatus(setup2.runId, step2Status)
      updatedStepList.left.value should ===(NotAllowedOnFinishedSeq)
    }
  }
}
