package esw.gateway.server.metrics

import csw.command.api.messages.{CommandServiceHttpMessage, CommandServiceWebsocketMessage}
import csw.command.api.messages.CommandServiceHttpMessage.{Oneway, Query, Submit, Validate}
import csw.command.api.messages.CommandServiceWebsocketMessage.{QueryFinal, SubscribeCurrentState}
import esw.gateway.server.metrics.Metrics._

object CommandMetrics {
  private val validateLabel     = "validate"
  private val submitLabel       = "submit"
  private val onewayLabel       = "oneway"
  private val queryLabel        = "query"
  private val queryFinalLabel   = "query_final"
  private val currentStateLabel = "subscribe_current_state"

  private[metrics] val commandCounterMetricName = "gateway_command_service_requests_total"
  private lazy val commandCounter =
    counter(
      metricName = commandCounterMetricName,
      help = "Total command service requests passing through gateway",
      labelNames = "api"
    )

  private[metrics] val currentStateGaugeMetricName = "gateway_command_service_active_current_state_subscribers_total"
  private lazy val currentStateGauge =
    gauge(
      metricName = currentStateGaugeMetricName,
      help = "Total active current state subscribers",
      labelNames = "api"
    )

  private[metrics] val queryFinalGaugeMetricName = "gateway_command_service_active_query_final_requests_total"
  private lazy val queryFinalGauge =
    gauge(
      metricName = queryFinalGaugeMetricName,
      help = "Total active query final requests in progress",
      labelNames = "api"
    )

  private def inc(label: String) = commandCounter.labels(label).inc()

  def incCommandCounter(command: CommandServiceHttpMessage): Unit =
    command match {
      case _: Validate => inc(validateLabel)
      case _: Submit   => inc(submitLabel)
      case _: Oneway   => inc(onewayLabel)
      case _: Query    => inc(queryLabel)
    }

  def incCommandGauge(command: CommandServiceWebsocketMessage): Unit =
    command match {
      case _: QueryFinal            => incQueryFinalGauge()
      case _: SubscribeCurrentState => incCurrentStateGauge()
    }

  def decCommandGauge(command: CommandServiceWebsocketMessage): Unit =
    command match {
      case _: QueryFinal            => decQueryFinalGauge()
      case _: SubscribeCurrentState => decCurrentStateGauge()
    }

  private def incCurrentStateGauge(): Unit = currentStateGauge.labels(currentStateLabel).inc()
  private def decCurrentStateGauge(): Unit = currentStateGauge.labels(currentStateLabel).dec()

  private def incQueryFinalGauge(): Unit = queryFinalGauge.labels(queryFinalLabel).inc()
  private def decQueryFinalGauge(): Unit = queryFinalGauge.labels(queryFinalLabel).dec()
}
