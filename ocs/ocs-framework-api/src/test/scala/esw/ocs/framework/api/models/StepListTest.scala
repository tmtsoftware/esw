package esw.ocs.framework.api.models

import csw.params.commands._
import csw.params.core.models.{Id, Prefix}
import esw.ocs.framework.api.BaseTestSuite
import esw.ocs.framework.api.models.StepList.DuplicateIdsFound
import esw.ocs.framework.api.models.StepStatus.{Finished, InFlight, Pending}
import esw.ocs.framework.api.models.messages.StepListActionResponse._

class StepListTest extends BaseTestSuite {

  "empty" must {
    "create empty StepList" in {
      StepList.empty.steps shouldBe Nil
    }
  }

  "apply" must {
    "return a StepList" in {
      val setup1 = Setup(Prefix("ocs.move1"), CommandName("test1"), None)
      val setup2 = Setup(Prefix("ocs.move2"), CommandName("test2"), None)

      val stepList = StepList(Sequence(setup1, setup2)).right.value
      stepList.steps.length shouldBe 2
    }

    "fail when duplicate Ids provided" in {
      val setup1 = Setup(Prefix("ocs.move1"), CommandName("test1"), None)
      val setup2 = Setup(Prefix("ocs.move2"), CommandName("test2"), None).copy(runId = setup1.runId)

      val sequence = Sequence(setup1, setup2)
      StepList(sequence).left.value shouldBe DuplicateIdsFound
    }
  }

  "nextPending" must {
    "return next pending step" in {
      val setup1 = Setup(Prefix("ocs.move1"), CommandName("test1"), None)
      val setup2 = Setup(Prefix("ocs.move2"), CommandName("test2"), None)
      val step1  = Step(setup1, InFlight, hasBreakpoint = false)
      val step2  = Step(setup2, Pending, hasBreakpoint = false)

      val stepList = StepList(Id(), List(step1, step2))

      stepList.nextPending.value shouldBe step2
    }

    "return none when no pending step present" in {
      val setup1 = Setup(Prefix("ocs.move1"), CommandName("test1"), None)
      val setup2 = Setup(Prefix("ocs.move2"), CommandName("test2"), None)
      val step1  = Step(setup1, Finished, hasBreakpoint = false)
      val step2  = Step(setup2, InFlight, hasBreakpoint = false)

      val stepList = StepList(Id(), List(step1, step2))

      stepList.nextPending shouldBe None
    }
  }

  "isPaused" must {
    val setup1 = Setup(Prefix("ocs.move1"), CommandName("test1"), None)
    val setup2 = Setup(Prefix("ocs.move2"), CommandName("test2"), None)

    "return true when next step exists and it has a breakpoint" in {
      val step1 = Step(setup1, InFlight, hasBreakpoint = false)
      val step2 = Step(setup2, Pending, hasBreakpoint = true)

      val stepList = StepList(Id(), List(step1, step2))
      stepList.isPaused shouldBe true
    }

    "return false when next step exists but doesn't have breakpoint" in {
      val step1 = Step(setup1, InFlight, hasBreakpoint = false)
      val step2 = Step(setup2, Pending, hasBreakpoint = false)

      val stepList = StepList(Id(), List(step1, step2))
      stepList.isPaused shouldBe false
    }

    "return false when next step doesn't exist" in {
      val step1 = Step(setup1, InFlight, hasBreakpoint = false)

      val stepList = StepList(Id(), List(step1))
      stepList.isPaused shouldBe false
    }
  }

  "isInFlight" must {
    val setup1 = Setup(Prefix("ocs.move1"), CommandName("test1"), None)
    val setup2 = Setup(Prefix("ocs.move2"), CommandName("test2"), None)

    "return true when at least one InFlight step exist" in {
      val step1 = Step(setup1, InFlight, hasBreakpoint = false)
      val step2 = Step(setup2, Pending, hasBreakpoint = true)

      val stepList = StepList(Id(), List(step1, step2))
      stepList.isInFlight shouldBe true
    }

    "return false when no InFlight step exist" in {
      val step1 = Step(setup1, Finished, hasBreakpoint = false)
      val step2 = Step(setup2, Pending, hasBreakpoint = false)

      val stepList = StepList(Id(), List(step1, step2))
      stepList.isInFlight shouldBe false
    }
  }

  "nextExecutable" must {
    val setup1 = Setup(Prefix("ocs.move1"), CommandName("test1"), None)
    val setup2 = Setup(Prefix("ocs.move2"), CommandName("test2"), None)

    "return step when next step exists and is not paused" in {
      val step1 = Step(setup1, InFlight, hasBreakpoint = false)
      val step2 = Step(setup2, Pending, hasBreakpoint = false)

      val stepList = StepList(Id(), List(step1, step2))
      stepList.nextExecutable.value shouldBe step2
    }
    "return none when next step doesn't exist" in {
      val step1 = Step(setup1, InFlight, hasBreakpoint = false)

      val stepList = StepList(Id(), List(step1))
      stepList.nextExecutable shouldBe None
    }
    "return none when next step is paused" in {
      val step1 = Step(setup1, InFlight, hasBreakpoint = false)
      val step2 = Step(setup2, Pending, hasBreakpoint = true)

      val stepList = StepList(Id(), List(step1, step2))
      stepList.nextExecutable shouldBe None
    }
  }

  "isFinished" must {
    val setup1 = Setup(Prefix("ocs.move1"), CommandName("test1"), None)
    val setup2 = Setup(Prefix("ocs.move2"), CommandName("test2"), None)

    // todo: revisit this
    "return true when StepList is empty" in {
      val stepList = StepList(Id(), Nil)
      stepList.isFinished shouldBe true
    }

    "return true when all steps are Finished" in {
      val step1 = Step(setup1, Finished, hasBreakpoint = false)
      val step2 = Step(setup2, Finished, hasBreakpoint = false)

      val stepList = StepList(Id(), List(step1, step2))
      stepList.isFinished shouldBe true
    }

    "return false when any step is not Finished" in {
      val step1 = Step(setup1, Finished, hasBreakpoint = false)
      val step2 = Step(setup2, InFlight, hasBreakpoint = false)

      val stepList = StepList(Id(), List(step1, step2))
      stepList.isFinished shouldBe false
    }
  }

  "replace" must {
    val setup1 = Setup(Prefix("ocs.move1"), CommandName("test1"), None)
    val setup2 = Setup(Prefix("ocs.move2"), CommandName("test2"), None)
    val setup3 = Setup(Prefix("ocs.move3"), CommandName("test3"), None)
    val setup4 = Setup(Prefix("ocs.move4"), CommandName("test4"), None)
    val setup5 = Setup(Prefix("ocs.move5"), CommandName("test5"), None)

    "replace step with given list of steps when Id matches and is in Pending status" in {
      val step1 = Step(setup1, InFlight, hasBreakpoint = false)
      val step2 = Step(setup2, Pending, hasBreakpoint = false)
      val step3 = Step(setup3, Pending, hasBreakpoint = false)

      val id              = Id()
      val stepList        = StepList(id, List(step1, step2, step3))
      val updatedStepList = stepList.replace(setup2.runId, List(setup4, setup5))
      updatedStepList.response shouldBe Replaced
      updatedStepList.stepList shouldBe StepList(id, List(step1, Step(setup4), Step(setup5), step3))
    }

    "fail with ReplaceNotSupportedInThisStatus error when Id matches and is not in Pending status" in {
      val step1 = Step(setup1, Finished, hasBreakpoint = false)
      val step2 = Step(setup2, Finished, hasBreakpoint = false)
      val step3 = Step(setup3, Pending, hasBreakpoint = false)

      val stepList        = StepList(Id(), List(step1, step2, step3))
      val id              = setup2.runId
      val updatedStepList = stepList.replace(id, List(setup4, setup5))
      updatedStepList.response shouldBe ReplaceNotSupportedInThisStatus(id, Finished)
      updatedStepList.stepList shouldBe stepList
    }

    "fail with IdDoesNotExist error when provided Id does't exist in StepList" in {
      val step1 = Step(setup1, InFlight, hasBreakpoint = false)
      val step2 = Step(setup2, Pending, hasBreakpoint = false)

      val stepList = StepList(Id(), List(step1, step2))

      val invalidId       = Id()
      val updatedStepList = stepList.replace(invalidId, List(setup4, setup5))
      updatedStepList.response shouldBe IdDoesNotExist(invalidId)
      updatedStepList.stepList shouldBe stepList
    }

    "fail with NotAllowedOnFinishedSeq error when StepList is finished" in {
      val step1 = Step(setup1, Finished, hasBreakpoint = false)
      val step2 = Step(setup2, Finished, hasBreakpoint = false)

      val id              = Id()
      val stepList        = StepList(id, List(step1, step2))
      val updatedStepList = stepList.replace(setup2.runId, List(setup4, setup5))
      updatedStepList.response shouldBe NotAllowedOnFinishedSeq
      updatedStepList.stepList shouldBe stepList
    }
  }

  "prepend" must {
    val setup1 = Setup(Prefix("ocs.move1"), CommandName("test1"), None)
    val setup2 = Setup(Prefix("ocs.move2"), CommandName("test2"), None)
    val setup3 = Setup(Prefix("ocs.move3"), CommandName("test3"), None)
    val setup4 = Setup(Prefix("ocs.move4"), CommandName("test4"), None)

    "add provided steps before next pending step" in {
      val step1 = Step(setup1, InFlight, hasBreakpoint = false)
      val step2 = Step(setup2, Pending, hasBreakpoint = false)

      val id              = Id()
      val stepList        = StepList(id, List(step1, step2))
      val updatedStepList = stepList.prepend(List(setup3, setup4))
      updatedStepList.response shouldBe Prepended
      updatedStepList.stepList shouldBe StepList(id, List(step1, Step(setup3), Step(setup4), step2))
    }

    "add provided steps at the end of StepList when StepList doesn't have Pending step" in {
      val step1 = Step(setup1, InFlight, hasBreakpoint = false)
      val step2 = Step(setup2, InFlight, hasBreakpoint = false)

      val stepList = StepList(Id(), List(step1, step2))

      val updatedStepList = stepList.prepend(List(setup3))
      updatedStepList.response shouldBe Prepended
      updatedStepList.stepList shouldBe StepList(stepList.runId, List(step1, step2, Step(setup3)))
    }

    "fail with NotAllowedOnFinishedSeq error when StepList is finished" in {
      val step1 = Step(setup1, Finished, hasBreakpoint = false)
      val step2 = Step(setup2, Finished, hasBreakpoint = false)

      val stepList        = StepList(Id(), List(step1, step2))
      val updatedStepList = stepList.prepend(List(setup3))
      updatedStepList.response shouldBe NotAllowedOnFinishedSeq
      updatedStepList.stepList shouldBe stepList
    }

  }

  "append" must {
    val setup1 = Setup(Prefix("ocs.move1"), CommandName("test1"), None)
    val setup2 = Setup(Prefix("ocs.move2"), CommandName("test2"), None)
    val setup3 = Setup(Prefix("ocs.move3"), CommandName("test3"), None)
    val setup4 = Setup(Prefix("ocs.move4"), CommandName("test4"), None)

    "add provided steps at the end of StepList" in {
      val step1 = Step(setup1, InFlight, hasBreakpoint = false)
      val step2 = Step(setup2, Pending, hasBreakpoint = false)

      val id              = Id()
      val stepList        = StepList(id, List(step1, step2))
      val updatedStepList = stepList.append(List(setup3, setup4))
      updatedStepList.response shouldBe Added // fixme: revisit type?
      updatedStepList.stepList shouldBe StepList(id, List(step1, step2, Step(setup3), Step(setup4)))
    }

    "fail with NotAllowedOnFinishedSeq error when StepList is finished" in {
      val step1 = Step(setup1, Finished, hasBreakpoint = false)
      val step2 = Step(setup2, Finished, hasBreakpoint = false)

      val stepList        = StepList(Id(), List(step1, step2))
      val updatedStepList = stepList.append(List(setup3))
      updatedStepList.response shouldBe NotAllowedOnFinishedSeq
      updatedStepList.stepList shouldBe stepList
    }
  }

  "delete" must {
    val setup1 = Setup(Prefix("ocs.move1"), CommandName("test1"), None)
    val setup2 = Setup(Prefix("ocs.move2"), CommandName("test2"), None)
    val setup3 = Setup(Prefix("ocs.move3"), CommandName("test3"), None)
    val setup4 = Setup(Prefix("ocs.move4"), CommandName("test4"), None)

    "return deleted and not deleted ids" in {
      val step1 = Step(setup1, Finished, hasBreakpoint = false)
      val step2 = Step(setup2, Pending, hasBreakpoint = false)
      val step3 = Step(setup3, InFlight, hasBreakpoint = false)
      val step4 = Step(setup4, Pending, hasBreakpoint = false)

      val id              = Id()
      val stepList        = StepList(id, List(step1, step2, step3, step4))
      val updatedStepList = stepList.delete(Set(setup2.runId, setup3.runId, setup4.runId))
      val deletionResult  = updatedStepList.response.asInstanceOf[DeletionResult]
      deletionResult.deleted.toSet shouldBe Set(setup2.runId, setup4.runId)
      deletionResult.notDeleted.toSet shouldBe Set(setup3.runId)
      updatedStepList.stepList shouldBe StepList(id, List(step1, step3))
    }

    "return not deleted ids when provided ids does not exist" in {
      val step1 = Step(setup1, Pending, hasBreakpoint = false)

      val stepList        = StepList(Id(), List(step1))
      val idsToBeDeleted  = Set(Id(), Id())
      val updatedStepList = stepList.delete(idsToBeDeleted)
      val deletionResult  = updatedStepList.response.asInstanceOf[DeletionResult]
      deletionResult.deleted shouldBe Nil
      deletionResult.notDeleted.toSet shouldBe idsToBeDeleted
      updatedStepList.stepList shouldBe stepList
    }

    "fail with NotAllowedOnFinishedSeq error when StepList is finished" in {
      val step1 = Step(setup1, Finished, hasBreakpoint = false)
      val step2 = Step(setup2, Finished, hasBreakpoint = false)

      val id              = Id()
      val stepList        = StepList(id, List(step1, step2))
      val updatedStepList = stepList.delete(Set(setup1.runId))
      updatedStepList.response shouldBe NotAllowedOnFinishedSeq
      updatedStepList.stepList shouldBe stepList
    }
  }

}
