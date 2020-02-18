package esw.gateway.server.metrics

import akka.NotUsed
import akka.http.scaladsl.model.ws.Message
import akka.stream.scaladsl.Source
import esw.gateway.api.protocol.{PostRequest, WebsocketRequest}

import scala.concurrent.ExecutionContext

sealed trait GatewayMetrics {
  def withMetrics(request: PostRequest): PostRequest
  def withMetrics(request: WebsocketRequest)(handler: WebsocketRequest => Source[Message, NotUsed]): Source[Message, NotUsed]
}

object GatewayMetrics {

  val NoOp = new GatewayMetrics {
    override def withMetrics(request: PostRequest): PostRequest = request
    override def withMetrics(request: WebsocketRequest)(
        handler: WebsocketRequest => Source[Message, NotUsed]
    ): Source[Message, NotUsed] = handler(request)
  }

}

class GatewayMetricsImpl(implicit ec: ExecutionContext) extends GatewayMetrics {
  import CommandMetrics._
  import EventMetrics._

  def withMetrics(request: PostRequest): PostRequest = {
    request match {
      case PostRequest.ComponentCommand(componentId, command) => incCommandCounter(command)
      case PostRequest.SequencerCommand(componentId, command) =>
      case PostRequest.PublishEvent(event)                    => incEventCounter(publishEventLabel)
      case PostRequest.GetEvent(eventKeys)                    => incEventCounter(getEventLabel)
      case PostRequest.SetAlarmSeverity(alarmKey, severity)   =>
      case PostRequest.Log(prefix, level, message, metadata)  =>
      case PostRequest.SetLogLevel(componentId, level)        =>
      case PostRequest.GetLogMetadata(componentId)            =>
    }

    request
  }

  def withMetrics(request: WebsocketRequest)(handler: WebsocketRequest => Source[Message, NotUsed]): Source[Message, NotUsed] = {
    lazy val response = handler(request)
    request match {
      case WebsocketRequest.ComponentCommand(componentId, command) => incCommandCounter(command); response

      case WebsocketRequest.SequencerCommand(componentId, command) => response

      case WebsocketRequest.Subscribe(eventKeys, maxFrequency) =>
        incSubscriberGuage()
        response.watchTermination() {
          case (mat, completion) =>
            completion.onComplete(_ => decSubscriberGuage())
            mat
        }

      case WebsocketRequest.SubscribeWithPattern(subsystem, maxFrequency, pattern) =>
        incPatternSubscriberGuage(subsystem, pattern)
        response.watchTermination() {
          case (mat, completion) =>
            completion.onComplete(_ => decPatternSubscriberGuage(subsystem, pattern))
            mat
        }
    }
  }

}
