package esw.ocs.framework.api.models

import csw.params.commands._
import csw.params.core.models.{Id, Prefix}
import esw.ocs.framework.api.BaseTestSuite
import esw.ocs.framework.api.models.StepList.DuplicateIdsFound
import esw.ocs.framework.api.models.StepStatus.{Finished, InFlight, Pending}
import esw.ocs.framework.api.models.messages.StepListActionResponse.{Added, IdDoesNotExist, NotAllowedOnFinishedSeq, Replaced}

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
      stepList.steps.length should ===(2)
    }

    "fail when duplicate Ids provided" in {
      val setup1 = Setup(Prefix("ocs.move1"), CommandName("test1"), None)
      val setup2 = Setup(Prefix("ocs.move2"), CommandName("test2"), None).copy(runId = setup1.runId)

      val sequence = Sequence(setup1, setup2)
      StepList(sequence).left.value should ===(DuplicateIdsFound)
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

      val id               = Id()
      val stepList         = StepList(id, List(step1, step2, step3))
      val replacedStepList = stepList.replace(setup2.runId, List(setup4, setup5))
      replacedStepList.response shouldBe Replaced
      replacedStepList.stepList shouldBe StepList(id, List(step1, Step(setup4), Step(setup5), step3))
    }

    // fixme
    "fail with ReplaceFailed error when Id matches and is not in Pending status" ignore {
      val step1 = Step(setup1, InFlight, hasBreakpoint = false)
      val step2 = Step(setup2, Pending, hasBreakpoint = false)
      val step3 = Step(setup3, Pending, hasBreakpoint = false)

      val id               = Id()
      val stepList         = StepList(id, List(step1, step2, step3))
      val replacedStepList = stepList.replace(setup2.runId, List(setup4, setup5))
      replacedStepList.response shouldBe Replaced
      replacedStepList.stepList shouldBe StepList(id, List(step1, Step(setup4), Step(setup5), step3))
    }

    "fail with IdDoesNotExist error when provided Id does't exist in StepList" in {
      val step1 = Step(setup1, InFlight, hasBreakpoint = false)
      val step2 = Step(setup2, Pending, hasBreakpoint = false)

      val stepList = StepList(Id(), List(step1, step2))

      val invalidId        = Id()
      val replacedStepList = stepList.replace(invalidId, List(setup4, setup5))
      replacedStepList.response shouldBe IdDoesNotExist(invalidId)
      replacedStepList.stepList shouldBe stepList
    }

    "fail with NotAllowedOnFinishedSeq error when StepList is finished" in {
      val step1 = Step(setup1, Finished, hasBreakpoint = false)
      val step2 = Step(setup2, Finished, hasBreakpoint = false)

      val id               = Id()
      val stepList         = StepList(id, List(step1, step2))
      val replacedStepList = stepList.replace(setup2.runId, List(setup4, setup5))
      replacedStepList.response shouldBe NotAllowedOnFinishedSeq
      replacedStepList.stepList shouldBe stepList
    }
  }

  "append" must {
    "allow adding commands when StepList is not finished" in {
      val setup           = Setup(Prefix("ocs.move"), CommandName("test"), None)
      val initialStepList = StepList.apply(Sequence(setup)).right.value

      val appendResult = initialStepList.append(List(setup))

      appendResult.response shouldBe Added
      appendResult.stepList should ===(initialStepList.copy(steps = Step.apply(setup) :: initialStepList.steps))
    }
  }
}
