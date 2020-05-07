package esw.ocs.testkit

import csw.location.api.models.Connection.HttpConnection
import csw.location.api.models.{ComponentId, ComponentType, HttpLocation}
import csw.location.api.scaladsl.LocationService
import csw.prefix.models.Prefix
import esw.ocs.testkit.utils.BaseTestSuite

import scala.concurrent.duration.DurationDouble

trait LocationUtils extends BaseTestSuite {

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(10.seconds)

  def locationService: LocationService

  def resolveHTTPLocation(prefix: Prefix, componentType: ComponentType): HttpLocation =
    locationService
      .resolve(HttpConnection(ComponentId(prefix, componentType)), 5.seconds)
      .futureValue
      .value

}
