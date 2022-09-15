/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.backend.testkit.stubs

import akka.Done
import csw.alarm.models.{AlarmSeverity, Key}
import esw.gateway.api.AlarmApi

import scala.concurrent.Future

class AlarmStubImpl extends AlarmApi {
  override def setSeverity(alarmKey: Key.AlarmKey, severity: AlarmSeverity): Future[Done] = Future.successful(Done)
}
