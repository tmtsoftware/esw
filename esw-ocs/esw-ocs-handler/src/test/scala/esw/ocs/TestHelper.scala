/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs

import esw.ocs.api.protocol.SequencerRequest

object TestHelper {
  implicit class Narrower(x: SequencerRequest) {
    def narrow: SequencerRequest = x
  }
}
