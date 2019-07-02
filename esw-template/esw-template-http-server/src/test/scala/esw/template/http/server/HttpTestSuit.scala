package esw.template.http.server

import akka.http.scaladsl.testkit.ScalatestRouteTest
import esw.template.http.server.commons.JsonSupportExt

trait HttpTestSuit extends BaseTestSuit with ScalatestRouteTest with JsonSupportExt
