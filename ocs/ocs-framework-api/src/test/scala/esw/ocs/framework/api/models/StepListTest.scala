package esw.ocs.framework.api.models

import csw.params.commands._
import csw.params.core.models.Prefix
import esw.ocs.framework.api.BaseTestSuite
import esw.ocs.framework.api.models.StepList.DuplicateIdsFound
import esw.ocs.framework.api.models.messages.StepListActionResponse.Added

class StepListTest extends BaseTestSuite {

  "StepList.from" must {
    "succeed when valid sequence provided" in {
      val setup1 = Setup(Prefix("ocs.move1"), CommandName("test1"), None)
      val setup2 = Setup(Prefix("ocs.move2"), CommandName("test2"), None)

      val stepList = StepList.from(Sequence(setup1, setup2)).right.value
      stepList.steps.length should ===(2)
    }

    "fail when duplicate Ids provided" in {
      val setup1 = Setup(Prefix("ocs.move1"), CommandName("test1"), None)
      val setup2 = Setup(Prefix("ocs.move2"), CommandName("test2"), None)
        .copy(runId = setup1.runId)

      val sequence = Sequence(setup1, setup2)
      StepList.from(sequence).left.value shouldBe DuplicateIdsFound
    }
  }

  "StepList's append" must {
    "allow adding commands when StepList is not finished" in {
      val setup           = Setup(Prefix("ocs.move"), CommandName("test"), None)
      val initialStepList = StepList.from(Sequence(setup)).right.value

      val appendResult = initialStepList.append(List(setup))

      appendResult.response shouldBe Added
      appendResult.stepList should ===(initialStepList.copy(steps = Step.from(setup) :: initialStepList.steps))
    }
  }

}
