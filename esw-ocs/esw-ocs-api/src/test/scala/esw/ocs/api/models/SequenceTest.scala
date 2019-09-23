package esw.ocs.api.models

import csw.params.commands.{CommandName, Observe, Sequence, Setup}
import csw.params.core.models.Prefix
import esw.ocs.api.BaseTestSuite

class SequenceTest extends BaseTestSuite {

  "apply" must {
    "create sequence from provided list of commands" in {
      val setup    = Setup(Prefix("esw.test1"), CommandName("setup-test"), None)
      val observe  = Observe(Prefix("esw.test1"), CommandName("observe-test"), None)
      val sequence = Sequence(setup, observe)

      sequence.commands should ===(List(setup, observe))
    }
  }

  "add" must {
    "allow adding list of commands to existing sequence" in {
      val setup    = Setup(Prefix("esw.test1"), CommandName("setup-test"), None)
      val observe  = Observe(Prefix("esw.test2"), CommandName("observe-test"), None)
      val sequence = Sequence(setup, observe)

      val newSetup   = Setup(Prefix("esw.test3"), CommandName("setup-test"), None)
      val newObserve = Observe(Prefix("esw.test4"), CommandName("setup-test"), None)

      val updateSequence = sequence.add(newSetup, newObserve)
      updateSequence.commands should ===(List(setup, observe, newSetup, newObserve))
    }

    "allow adding new sequence to existing sequence" in {
      val setup    = Setup(Prefix("esw.test1"), CommandName("setup-test"), None)
      val observe  = Observe(Prefix("esw.test2"), CommandName("observe-test"), None)
      val sequence = Sequence(setup, observe)

      val newSetup    = Setup(Prefix("esw.test3"), CommandName("setup-test"), None)
      val newObserve  = Observe(Prefix("esw.test4"), CommandName("observe-test"), None)
      val newSequence = Sequence(newSetup, newObserve)

      val updateSequence = sequence.add(newSequence)
      updateSequence.commands should ===(List(setup, observe, newSetup, newObserve))
    }
  }
}
