/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.dsl.epics

data class InvalidStateException(val name: String) : RuntimeException("Failed transition to invalid state: $name")