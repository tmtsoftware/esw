/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.dsl.script.exceptions

class ScriptInitialisationFailedException(msg: String) extends RuntimeException(s"Script initialization failed with : $msg")
