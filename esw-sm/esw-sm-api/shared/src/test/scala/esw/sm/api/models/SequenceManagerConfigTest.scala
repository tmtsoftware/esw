package esw.sm.api.models

import csw.prefix.models.Subsystem._
import esw.sm.api.models.CommonFailure.ConfigurationMissing
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class SequenceManagerConfigTest extends AnyWordSpecLike with Matchers with TypeCheckedTripleEquals {

  private val DarkNight  = "darknight"
  private val ClearSkies = "clearskies"
  private val ConfigMap = Map(
    DarkNight  -> ObsModeConfig(Resources("r1", "r2"), Sequencers(ESW, TCS)),
    ClearSkies -> ObsModeConfig(Resources("r3", "r4"), Sequencers(AOESW, IRIS))
  )

  "Resources needed for observing mode" must {
    "create from strings" in {
      val resources = Resources("IRIS", "WFOS")

      resources.resources should ===(Set("IRIS", "WFOS"))
    }

    "check conflictsWith resources" in {
      val resources               = Resources("IRIS", "WFOS")
      val conflictingResources    = Resources("IRIS", "AOS")
      val nonConflictingResources = Resources("TCS", "NFIRAOS")

      resources.conflictsWith(conflictingResources) should ===(true)
      resources.conflictsWith(nonConflictingResources) should ===(false)
    }
  }

  "Sequencers needed for observing mode" must {
    "create from subsystems" in {
      val sequencers = Sequencers(ESW, TCS, NFIRAOS)

      sequencers.subsystems should ===(List(ESW, TCS, NFIRAOS))
    }
  }

  "Resources for SequenceManagerConfig" must {

    "return resources if obsMode present in map | ESW-162" in {
      val sequenceManagerConfig = SequenceManagerConfig(ConfigMap)

      sequenceManagerConfig.resources(DarkNight) shouldBe Right(Resources("r1", "r2"))
    }

    "return ConfigurationMissing if obsMode not present in map | ESW-162" in {
      val sequenceManagerConfig = SequenceManagerConfig(ConfigMap)

      sequenceManagerConfig.resources("RandomObsMode") shouldBe Left(ConfigurationMissing("RandomObsMode"))
    }
  }

  "Sequencers for SequenceManagerConfig" must {

    "return sequencers if obsMode present in map | ESW-162" in {
      val sequenceManagerConfig = SequenceManagerConfig(ConfigMap)

      sequenceManagerConfig.sequencers(DarkNight) shouldBe Right(Sequencers(ESW, TCS))
    }

    "return ConfigurationMissing if obsMode not present in map | ESW-162" in {
      val sequenceManagerConfig = SequenceManagerConfig(ConfigMap)

      sequenceManagerConfig.sequencers("RandomObsMode") shouldBe Left(ConfigurationMissing("RandomObsMode"))
    }
  }
}
