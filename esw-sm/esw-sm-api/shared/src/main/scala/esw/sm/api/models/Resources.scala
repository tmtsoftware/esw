/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.sm.api.models

import csw.prefix.models.Subsystem

case class Resource(subsystem: Subsystem)

case class Resources(resources: Set[Resource]) {
  private def conflictsWith(other: Resources): Boolean  = this.resources.exists(other.resources.contains)
  def conflictsWithAny(others: Set[Resources]): Boolean = others.exists(conflictsWith)
}

object Resources {
  def apply(resources: Resource*): Resources = new Resources(resources.toSet)
}
