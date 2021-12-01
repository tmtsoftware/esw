package esw.sm.impl.config

import csw.prefix.models.Subsystem.*
import esw.ocs.api.models.{ObsMode, Variation, VariationId}
import esw.sm.api.models.{Resource, Resources, VariationIds}
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
  private val eswSequencerId            = VariationId(ESW)
  private val tcsSequencerId            = VariationId(TCS)
  private val aoeswSequencerId          = VariationId(AOESW)
  private val irisSequencerId           = VariationId(IRIS)
  private val irisSequencerIdWithImager = VariationId(IRIS, Some(Variation("IRIS_IMAGER")))
  private val irisSequencerIdWithIFS    = VariationId(IRIS, Some(Variation("IRIS_IFS")))
  private val nfiraosSequencerId        = VariationId(NFIRAOS)

  private val ConfigMap = Map(
    DarkNight  -> ObsModeConfig(Resources(esw, tcs), VariationIds(eswSequencerId, tcsSequencerId)),
    ClearSkies -> ObsModeConfig(Resources(aoesw, iris), VariationIds(aoeswSequencerId, irisSequencerId)),
    IRISImagerAndIFS -> ObsModeConfig(
      Resources(tcs, iris),
      VariationIds(
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
      val sequencers = VariationIds(eswSequencerId, tcsSequencerId, nfiraosSequencerId)

      sequencers.variationIds should ===(List(eswSequencerId, tcsSequencerId, nfiraosSequencerId))
    }
    "create from subsystems and variation | ESW-561" in {
      val eswSequencerId     = VariationId(ESW)
      val tcsSequencerId     = VariationId(TCS, Some(Variation("Variation1")))
      val nfiraosSequencerId = VariationId(NFIRAOS, Some(Variation("Variation1")))
      val sequencers         = VariationIds(eswSequencerId, tcsSequencerId, nfiraosSequencerId)

      sequencers.variationIds should ===(List(eswSequencerId, tcsSequencerId, nfiraosSequencerId))
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
      sequenceManagerConfig.sequencers(DarkNight) should ===(Some(VariationIds(eswSequencerId, tcsSequencerId)))
    }

    "return sequencers with variation if obsMode present in map | ESW-561" in {
      val sequenceManagerConfig = SequenceManagerConfig(ConfigMap)
      sequenceManagerConfig.sequencers(IRISImagerAndIFS) should ===(
        Some(
          VariationIds(
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
