package esw.gateway.server.metrics

import com.lonelyplanet.prometheus.PrometheusResponseTimeRecorder
import com.lonelyplanet.prometheus.api.MetricsEndpoint
import com.lonelyplanet.prometheus.directives.ResponseTimeRecordingDirectives
import io.prometheus.client.{CollectorRegistry, Counter, Gauge}

object Metrics {
  private val prometheusRegistry: CollectorRegistry                          = PrometheusResponseTimeRecorder.DefaultRegistry
  private val prometheusResponseTimeRecorder: PrometheusResponseTimeRecorder = PrometheusResponseTimeRecorder.Default
  private val responseTimeDirectives                                         = ResponseTimeRecordingDirectives(prometheusResponseTimeRecorder)

  val metricsEndpoint: MetricsEndpoint = new MetricsEndpoint(prometheusRegistry)

  def counter(metricName: String, help: String, labelNames: String*): Counter =
    Counter
      .build()
      .name(metricName)
      .help(help)
      .labelNames(labelNames: _*)
      .register(prometheusRegistry)

  def guage(metricName: String, help: String, labelNames: String*): Gauge =
    Gauge
      .build()
      .name(metricName)
      .help(help)
      .labelNames(labelNames: _*)
      .register(prometheusRegistry)
}
