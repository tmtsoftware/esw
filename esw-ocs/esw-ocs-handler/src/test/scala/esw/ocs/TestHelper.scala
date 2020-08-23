package esw.ocs

import esw.ocs.api.protocol.SequencerRequest

object TestHelper {
  implicit class Narrower(x: SequencerRequest) {
    def narrow: SequencerRequest = x
  }
}
