/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.sm.api.models

sealed trait ResourceStatus

object ResourceStatus {

  case object InUse extends ResourceStatus

  case object Available extends ResourceStatus
}
