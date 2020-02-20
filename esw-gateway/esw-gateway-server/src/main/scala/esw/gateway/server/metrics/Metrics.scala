package esw.gateway.server.metrics

import akka.http.scaladsl.server.Route
import com.lonelyplanet.prometheus.PrometheusResponseTimeRecorder
import com.lonelyplanet.prometheus.api.MetricsEndpoint
import io.prometheus.client.{CollectorRegistry, Counter, Gauge}

object Metrics {
  private[metrics] val prometheusRegistry: CollectorRegistry = PrometheusResponseTimeRecorder.DefaultRegistry

  val metricsRoute: Route = new MetricsEndpoint(prometheusRegistry).routes

  private[metrics] val httpCounterMetricName = "gateway_http_requests_total"
  val httpCounter: Counter =
    counter(
      metricName = httpCounterMetricName,
      help = "Total http requests",
      labelNames = "msg"
    )

  private[metrics] val websocketGaugeMetricName = "gateway_websocket_active_request_total"
  val websocketGauge: Gauge =
    gauge(
      metricName = websocketGaugeMetricName,
      help = "Total active websocket connections",
      labelNames = "msg"
    )

  def counter(metricName: String, help: String, labelNames: String*): Counter =
    Counter
      .build()
      .name(metricName)
      .help(help)
      .labelNames(labelNames: _*)
      .register(prometheusRegistry)

  def gauge(metricName: String, help: String, labelNames: String*): Gauge =
    Gauge
      .build()
      .name(metricName)
      .help(help)
      .labelNames(labelNames: _*)
      .register(prometheusRegistry)

  def createLabel[A](obj: A): String = {
    val name = obj.getClass.getSimpleName
    if (name.endsWith("$")) name.dropRight(1) else name
  }
  def createLabel[A, B](obj1: A, obj2: B): String = createLabel(obj1) + "_" + createLabel(obj2)
}
