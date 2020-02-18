package esw.gateway.server.metrics

import csw.command.api.messages.{CommandServiceHttpMessage, CommandServiceWebsocketMessage}
import csw.command.api.messages.CommandServiceHttpMessage.{Oneway, Query, Submit, Validate}
import esw.gateway.server.metrics.Metrics._

object CommandMetrics {
  private val validateLabel       = "validate"
  private val submitLabel         = "submit"
  private val onewayLabel         = "oneway"
  private val queryLabel          = "query"
  private val queryFinalLabel     = "query_final"
  private val subscribeStateLabel = "subscribe_current_state"

  private lazy val commandCounter =
    counter(
      metricName = "gateway_command_service_requests_total",
      help = "Total command service requests through gateway",
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

  def incCommandCounter(command: CommandServiceWebsocketMessage): Unit =
    command match {
      case _: CommandServiceWebsocketMessage.QueryFinal            => inc(queryFinalLabel)
      case _: CommandServiceWebsocketMessage.SubscribeCurrentState => inc(subscribeStateLabel)
    }
}
