package esw.sm.impl.config

import csw.prefix.models.Subsystem.*
import esw.ocs.api.models.{ObsMode, SequencerId}
import esw.sm.api.models.{Resource, Resources, SequencerIds}
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class SequenceManagerConfigTest extends AnyWordSpecLike with Matchers with TypeCheckedTripleEquals {

  private val DarkNight        = ObsMode("darknight")
  private val ClearSkies       = ObsMode("clearskies")
  private val IRISImagerAndIFS = ObsMode("IRIS_ImagerAndIFS")

  private val esw: Resource             = Resource(ESW)
  private val tcs: Resource             = Resource(TCS)
  private val aoesw: Resource           = Resource(AOESW)
  private val iris: Resource            = Resource(IRIS)
  private val nfiraos: Resource         = Resource(NFIRAOS)
  private val eswSequencerId            = SequencerId(ESW)
  private val tcsSequencerId            = SequencerId(TCS)
  private val aoeswSequencerId          = SequencerId(AOESW)
  private val irisSequencerId           = SequencerId(IRIS)
  private val irisSequencerIdWithImager = SequencerId(IRIS, Some("IRIS_IMAGER"))
  private val irisSequencerIdWithIFS    = SequencerId(IRIS, Some("IRIS_IFS"))
  private val nfiraosSequencerId        = SequencerId(NFIRAOS)

  private val ConfigMap = Map(
    DarkNight  -> ObsModeConfig(Resources(esw, tcs), SequencerIds(eswSequencerId, tcsSequencerId)),
    ClearSkies -> ObsModeConfig(Resources(aoesw, iris), SequencerIds(aoeswSequencerId, irisSequencerId)),
    IRISImagerAndIFS -> ObsModeConfig(
      Resources(tcs, iris),
      SequencerIds(
        eswSequencerId,
        irisSequencerId,
        irisSequencerIdWithIFS,
        irisSequencerIdWithImager
      )
    )
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
      val sequencers = SequencerIds(eswSequencerId, tcsSequencerId, nfiraosSequencerId)

      sequencers.sequencerIds should ===(List(eswSequencerId, tcsSequencerId, nfiraosSequencerId))
    }
    "create from subsystems and variation | ESW-561" in {
      val eswSequencerId     = SequencerId(ESW)
      val tcsSequencerId     = SequencerId(TCS, Some("Variation1"))
      val nfiraosSequencerId = SequencerId(NFIRAOS, Some("Variation1"))
      val sequencers         = SequencerIds(eswSequencerId, tcsSequencerId, nfiraosSequencerId)

      sequencers.sequencerIds should ===(List(eswSequencerId, tcsSequencerId, nfiraosSequencerId))
    }
  }

  "Resources for SequenceManagerConfig" must {

    "return resources if obsMode present in map | ESW-162" in {
      val sequenceManagerConfig = SequenceManagerConfig(ConfigMap)
      sequenceManagerConfig.resources(DarkNight) should ===(Some(Resources(esw, tcs)))
    }

    "return ConfigurationMissing if obsMode not present in map while fetching Resources | ESW-162" in {
      val sequenceManagerConfig = SequenceManagerConfig(ConfigMap)
      sequenceManagerConfig.resources(ObsMode("RandomObsMode")) should ===(None)
    }
  }

  "Sequencers for SequenceManagerConfig" must {

    "return sequencers if obsMode present in map | ESW-162" in {
      val sequenceManagerConfig = SequenceManagerConfig(ConfigMap)
      sequenceManagerConfig.sequencers(DarkNight) should ===(Some(SequencerIds(eswSequencerId, tcsSequencerId)))
    }

    "return sequencers with variation if obsMode present in map | ESW-561" in {
      val sequenceManagerConfig = SequenceManagerConfig(ConfigMap)
      sequenceManagerConfig.sequencers(IRISImagerAndIFS) should ===(
        Some(
          SequencerIds(
            eswSequencerId,
            irisSequencerId,
            irisSequencerIdWithIFS,
            irisSequencerIdWithImager
          )
        )
      )
    }

    "return ConfigurationMissing if obsMode not present in map while fetching Sequencers | ESW-162" in {
      val sequenceManagerConfig = SequenceManagerConfig(ConfigMap)
      sequenceManagerConfig.sequencers(ObsMode("RandomObsMode")) should ===(None)
    }
  }
}
