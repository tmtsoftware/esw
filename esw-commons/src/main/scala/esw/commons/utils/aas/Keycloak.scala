/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.commons.utils.aas

import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.Service
import csw.location.api.models.Connection.HttpConnection
import csw.location.api.scaladsl.LocationService
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.CSW
import org.tmt.embedded_keycloak.utils.BearerToken

import scala.concurrent.{ExecutionContext, Future}

// FIXME: This should be moved to test scope or appropriate location
// $COVERAGE-OFF$
class Keycloak(locationService: LocationService)(implicit ec: ExecutionContext) {
  private val Realm  = "TMT"
  private val Client = "tmt-frontend-app"

  def getToken(userName: String, password: String): Future[String] =
    locationService.find(HttpConnection(ComponentId(Prefix(CSW, "AAS"), Service))).map { locOpt =>
      val location = locOpt.getOrElse(throw new RuntimeException("KeyCloak is not up"))
      val host     = location.uri.getHost
      val port     = location.uri.getPort
      BearerToken.fromServer(port, userName, password, Realm, Client, host).token
    }
}
// $COVERAGE-ON$
