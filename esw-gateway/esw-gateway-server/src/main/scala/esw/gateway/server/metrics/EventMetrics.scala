package esw.gateway.server.metrics

import csw.prefix.models.Subsystem
import esw.gateway.server.metrics.Metrics._

object EventMetrics {
  val getEventLabel              = "get_event"
  val publishEventLabel          = "publish_event"
  val subscribeEventLabel        = "subscribe_event"
  val subscribePatternEventLabel = "subscribe_pattern_event"

  private lazy val eventCounter =
    counter(
      metricName = "gateway_event_service_requests_total",
      help = "Total event service requests passing through gateway",
      labelNames = "api"
    )

  private lazy val subscribeGauge =
    gauge(
      metricName = "gateway_event_service_active_subscribers_total",
      help = "Total active event subscribers",
      labelNames = "api"
    )

  private lazy val patternSubscribeGauge =
    gauge(
      metricName = "gateway_event_service_active_pattern_subscribers_total",
      help = "Total active pattern event subscribers",
      labelNames = "api",
      "subsystem",
      "pattern"
    )

  def incEventCounter(label: String): Unit = eventCounter.labels(label).inc()

  def incSubscriberGauge(): Unit = subscribeGauge.labels(subscribeEventLabel).inc()
  def decSubscriberGauge(): Unit = subscribeGauge.labels(subscribeEventLabel).dec()

  def incPatternSubscriberGauge(subsystem: Subsystem, pattern: String): Unit =
    patternSubscribeGauge.labels(subscribeEventLabel, subsystem.name, pattern).inc()
  def decPatternSubscriberGauge(subsystem: Subsystem, pattern: String): Unit =
    patternSubscribeGauge.labels(subscribeEventLabel, subsystem.name, pattern).dec()
}
