package esw.ocs.testkit

import csw.location.api.models.Connection.HttpConnection
import csw.location.api.models.{ComponentId, ComponentType, HttpLocation}
import csw.location.api.scaladsl.LocationService
import csw.prefix.models.Prefix
import org.scalatest.OptionValues._
import org.scalatest.concurrent.ScalaFutures._

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{ExecutionContext, Future}

trait LocationUtils {

  def locationService: LocationService
  implicit def ec: ExecutionContext

  def resolveHTTPLocationAsync(prefix: Prefix, componentType: ComponentType): Future[HttpLocation] =
    locationService.resolve(HttpConnection(ComponentId(prefix, componentType)), 5.seconds).map(_.value)

  def resolveHTTPLocation(prefix: Prefix, componentType: ComponentType): HttpLocation =
    resolveHTTPLocationAsync(prefix, componentType).futureValue

}
