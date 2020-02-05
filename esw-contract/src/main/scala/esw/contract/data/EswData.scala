package esw.contract.data

import csw.contract.generator.Services
import esw.contract.data.sequencer.SequencerContract

object EswData {
  val services: Services = Services(
    Map(
      "sequencer-service" -> SequencerContract.service
    )
  )
}
