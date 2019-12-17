package esw.http.core.wiring
import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.Flow
import fr.davit.akka.http.metrics.core.scaladsl.server.HttpMetricsDirectives._
import fr.davit.akka.http.metrics.core.scaladsl.server.{HttpMetricsRoute, HttpMetricsSettings}
import fr.davit.akka.http.metrics.prometheus.PrometheusRegistry
import fr.davit.akka.http.metrics.prometheus.marshalling.PrometheusMarshallers._
import io.prometheus.client.CollectorRegistry

object Metrics {
  private val settings: HttpMetricsSettings = HttpMetricsSettings.default
  private val prometheus: CollectorRegistry = CollectorRegistry.defaultRegistry
  private val registry: PrometheusRegistry  = PrometheusRegistry(settings, prometheus)

  private val metricsRoute: Route = (get & path("metrics"))(metrics(registry))

  def withMetrics(applicationRoute: Route)(implicit actorSystem: ActorSystem): Flow[HttpRequest, HttpResponse, NotUsed] =
    HttpMetricsRoute(applicationRoute ~ metricsRoute).recordMetrics(registry)
}
