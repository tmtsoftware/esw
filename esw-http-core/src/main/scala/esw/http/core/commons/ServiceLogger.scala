/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.http.core.commons

import csw.logging.client.scaladsl.LoggerFactory
import csw.prefix.models.Prefix

/**
 * All the logs generated from the service will have a fixed prefix, which is picked from configuration.
 * The prefix helps in production to filter out logs from a particular component and in this case,
 * it helps to filter out logs generated from the service that uses this template.
 */
class ServiceLogger(prefix: Prefix) extends LoggerFactory(prefix)
