package esw.ocs.framework.api.models

import csw.params.commands._
import csw.params.core.models.Prefix
import esw.ocs.framework.api.BaseTestSuite
import esw.ocs.framework.api.models.messages.StepListActionResponse.Added

class StepListTest extends BaseTestSuite {

  "StepList's append" must {
    "allow adding commands when StepList is not finished" in {
      val setup           = Setup(Prefix("ocs.move"), CommandName("test"), None)
      val initialStepList = StepList.from(Sequence(setup)).right.value

      val appendResult = initialStepList.append(List(setup))

      appendResult.reply shouldBe Added
      appendResult.stepList shouldBe initialStepList.copy(steps = Step.from(setup) :: initialStepList.steps)
    }
  }

}
