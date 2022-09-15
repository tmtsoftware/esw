/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.api.actor.messages

import esw.ocs.api.actor.messages.InternalSequencerState.*
import esw.testcommons.BaseTestSuite

class InternalSequencerStateTest extends BaseTestSuite {

  "name should return name of the state" in {
    Idle.name shouldEqual "Idle"
    Running.name shouldEqual "Running"
    Offline.name shouldEqual "Offline"
    Loaded.name shouldEqual "Loaded"
    GoingOffline.name shouldEqual "GoingOffline"
    GoingOnline.name shouldEqual "GoingOnline"
    Starting.name shouldEqual "Starting"
    Stopping.name shouldEqual "Stopping"
    Submitting.name shouldEqual "Submitting"
    AbortingSequence.name shouldEqual "AbortingSequence"
  }
}
