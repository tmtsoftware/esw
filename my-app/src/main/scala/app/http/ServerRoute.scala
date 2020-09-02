package app.http

import app.http.models.HttpCodecs
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import csw.aas.http.AuthorizationPolicy.RealmRolePolicy
import csw.aas.http.SecurityDirectives

class ServerRoute(sampleImpl: ServerImpl, securityDirectives: SecurityDirectives) extends HttpCodecs {

  val route: Route =
    path("sayHello") {
      complete(sampleImpl.sayHello())
    } ~
      path("securedSayHello") {
        securityDirectives.sGet(RealmRolePolicy("Esw-user")) { token =>
          complete(sampleImpl.securedSayHello())
        }
      }
}
