/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.scripts.examples.testData.sharedState

// ESW-185 - this mutable variable shared between main ThreadSafeScript and CounterIncrementer script
var counter = 0