/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.commons.utils.config

/**
 * This model represents failure while accessing Script Version from version conf.
 * @param msg - a hint about the failure cause.
 */
case class FetchingScriptVersionFailed(msg: String) extends RuntimeException(msg)
