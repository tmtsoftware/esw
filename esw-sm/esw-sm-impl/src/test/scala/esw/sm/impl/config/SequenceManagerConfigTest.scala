package esw.sm.impl.config

import csw.prefix.models.Subsystem
import csw.prefix.models.Subsystem._
import esw.ocs.api.models.ObsMode
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class SequenceManagerConfigTest extends AnyWordSpecLike with Matchers with TypeCheckedTripleEquals {

  private val DarkNight             = ObsMode("darknight")
  private val ClearSkies            = ObsMode("clearskies")
  private val SequencerStartRetries = 1

  private val esw: Subsystem     = ESW
  private val tcs: Subsystem     = TCS
  private val aoesw: Subsystem   = AOESW
  private val iris: Subsystem    = IRIS
  private val nfiraos: Subsystem = NFIRAOS

  private val ConfigMap = Map(
    DarkNight  -> ObsModeConfig(Resources(esw, tcs), Sequencers(ESW, TCS)),
    ClearSkies -> ObsModeConfig(Resources(aoesw, iris), Sequencers(AOESW, IRIS))
  )

  "Resources needed for observing mode" must {
    "create from one or more resource" in {
      val resources = Resources(iris, tcs)
      resources.resources should ===(Set(iris, tcs))
    }

    "check conflictsWithAny resources | ESW-168, ESW-170, ESW-179" in {
      val resources               = Resources(iris, esw)
      val conflictingResources    = Resources(iris, aoesw)
      val nonConflictingResources = Resources(tcs, nfiraos)

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
      val sequenceManagerConfig = SequenceManagerConfig(ConfigMap, SequencerStartRetries)
      sequenceManagerConfig.resources(DarkNight) should ===(Some(Resources(esw, tcs)))
    }

    "return ConfigurationMissing if obsMode not present in map while fetching Resources | ESW-162" in {
      val sequenceManagerConfig = SequenceManagerConfig(ConfigMap, SequencerStartRetries)
      sequenceManagerConfig.resources(ObsMode("RandomObsMode")) should ===(None)
    }
  }

  "Sequencers for SequenceManagerConfig" must {

    "return sequencers if obsMode present in map | ESW-162" in {
      val sequenceManagerConfig = SequenceManagerConfig(ConfigMap, SequencerStartRetries)
      sequenceManagerConfig.sequencers(DarkNight) should ===(Some(Sequencers(ESW, TCS)))
    }

    "return ConfigurationMissing if obsMode not present in map while fetching Sequencers | ESW-162" in {
      val sequenceManagerConfig = SequenceManagerConfig(ConfigMap, SequencerStartRetries)
      sequenceManagerConfig.sequencers(ObsMode("RandomObsMode")) should ===(None)
    }
  }
}
