package esw.gateway.server.metrics

import akka.http.scaladsl.server.Route
import csw.command.api.messages.CommandServiceHttpMessage.{Oneway, Query, Submit, Validate}
import csw.location.api.models.ComponentId
import csw.params.commands.Setup
import csw.params.core.models.Id
import csw.params.events.{Event, EventKey}
import esw.gateway.api.protocol.PostRequest
import esw.gateway.api.protocol.PostRequest.{ComponentCommand, GetEvent, PublishEvent}
import esw.gateway.server.handlers.PostHandlerImpl
import esw.gateway.server.metrics.CommandMetrics._
import esw.gateway.server.metrics.EventMetrics._
import esw.http.core.BaseTestSuite
import org.scalatest.prop.TableDrivenPropertyChecks._

class PostHandlerMetricsTest extends BaseTestSuite {
  private val mockedPostHandler  = mock[PostHandlerImpl]
  private val mockedRoute        = mock[Route]
  private val postHandlerMetrics = new PostHandlerMetrics(mockedPostHandler)

  private def getCounterValue(name: String, labels: Map[String, String]): Double =
    Metrics.prometheusRegistry.getSampleValue(name, labels.keys.toArray, labels.values.toArray)

  private def runCounterTest(postRequest: PostRequest, metricName: String, labelValue: String): Unit = {
    when(mockedPostHandler.handle(postRequest)).thenReturn(mockedRoute)
    def counterValue = getCounterValue(metricName, Map("api" -> labelValue))

    counterValue shouldBe 0
    (1 to 10).foreach(_ => postHandlerMetrics.handle(postRequest) shouldBe mockedRoute)
    counterValue shouldBe 10
  }

  "Command Service Metrics" must {
    val command     = mock[Setup]
    val componentId = mock[ComponentId]
    val id          = mock[Id]

    Table(
      ("CommandServiceHttpMessage", "Label"),
      (Validate(command), "validate"),
      (Submit(command), "submit"),
      (Oneway(command), "oneway"),
      (Query(id), "query")
    ).foreach {
      case (command, label) =>
        s"increment $label counter on every $label request | ESW-197" in {
          val postRequest: PostRequest = ComponentCommand(componentId, command)
          runCounterTest(postRequest, commandCounterMetricName, label)
        }
    }
  }

  "Event Service Metrics" must {
    val eventKey = mock[EventKey]
    val event    = mock[Event]

    Table(
      ("PostRequest", "Label"),
      (GetEvent(Set(eventKey)), "get_event"),
      (PublishEvent(event), "publish_event")
    ).foreach {
      case (request, label) =>
        s"increment $label counter on every $label request | ESW-197" in {
          runCounterTest(request, eventCounterMetricName, label)
        }
    }
  }
}
