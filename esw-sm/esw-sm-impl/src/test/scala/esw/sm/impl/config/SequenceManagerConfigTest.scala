package esw.sm.impl.config

import csw.prefix.models.Subsystem._
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class SequenceManagerConfigTest extends AnyWordSpecLike with Matchers with TypeCheckedTripleEquals {

  private val DarkNight             = "darknight"
  private val ClearSkies            = "clearskies"
  private val sequencerStartRetries = 1
  private val ConfigMap = Map(
    DarkNight  -> ObsModeConfig(Resources("r1", "r2"), Sequencers(ESW, TCS)),
    ClearSkies -> ObsModeConfig(Resources("r3", "r4"), Sequencers(AOESW, IRIS))
  )

  "Resources needed for observing mode" must {
    "create from strings" in {
      val resources = Resources("IRIS", "WFOS")
      resources.resources should ===(Set("IRIS", "WFOS"))
    }

    "check conflictsWithAny resources | ESW-168, ESW-170" in {
      val resources               = Resources("IRIS", "WFOS")
      val conflictingResources    = Resources("IRIS", "AOS")
      val nonConflictingResources = Resources("TCS", "NFIRAOS")

      resources.conflictsWithAny(Set(conflictingResources)) should ===(true)
      resources.conflictsWithAny(Set(nonConflictingResources)) should ===(false)
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
      val sequenceManagerConfig = SequenceManagerConfig(ConfigMap, sequencerStartRetries)
      sequenceManagerConfig.resources(DarkNight) should ===(Some(Resources("r1", "r2")))
    }

    "return ConfigurationMissing if obsMode not present in map | ESW-162" in {
      val sequenceManagerConfig = SequenceManagerConfig(ConfigMap, sequencerStartRetries)
      sequenceManagerConfig.resources("RandomObsMode") should ===(None)
    }
  }

  "Sequencers for SequenceManagerConfig" must {

    "return sequencers if obsMode present in map | ESW-162" in {
      val sequenceManagerConfig = SequenceManagerConfig(ConfigMap, sequencerStartRetries)
      sequenceManagerConfig.sequencers(DarkNight) should ===(Some(Sequencers(ESW, TCS)))
    }

    "return ConfigurationMissing if obsMode not present in map | ESW-162" in {
      val sequenceManagerConfig = SequenceManagerConfig(ConfigMap, sequencerStartRetries)
      sequenceManagerConfig.sequencers("RandomObsMode") should ===(None)
    }
  }
}
