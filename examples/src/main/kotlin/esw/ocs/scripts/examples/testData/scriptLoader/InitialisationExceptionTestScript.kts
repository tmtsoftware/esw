/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.scripts.examples.testData.scriptLoader

import esw.ocs.dsl.core.script

script {
    throw RuntimeException("initialisation failed")
}