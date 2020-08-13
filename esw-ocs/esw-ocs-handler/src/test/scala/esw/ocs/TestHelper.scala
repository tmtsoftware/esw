package esw.ocs

import esw.ocs.api.protocol.SequencerPostRequest

object TestHelper {
  implicit class Narrower(x: SequencerPostRequest) {
    def narrow: SequencerPostRequest = x
  }
}
