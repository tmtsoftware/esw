/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.sm.impl.utils

import csw.location.api.models.AkkaLocation
import csw.prefix.models.Prefix

object Types {
  type SeqCompPrefix   = Prefix
  type SeqCompLocation = AkkaLocation
  type SequencerPrefix = Prefix

  type AgentPrefix   = Prefix
  type AgentLocation = AkkaLocation
}
