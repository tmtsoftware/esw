/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.gateway.server.utils

import csw.location.api.models.ComponentId

case class ComponentNotFoundException(componentId: ComponentId)
    extends RuntimeException(s"No component is registered with id $componentId")
