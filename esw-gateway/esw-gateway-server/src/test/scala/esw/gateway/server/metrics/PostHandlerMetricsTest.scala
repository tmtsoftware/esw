package esw.gateway.server.metrics

import akka.http.scaladsl.server.Route
import csw.command.api.messages.CommandServiceHttpMessage.{Oneway, Query, Submit, Validate}
import csw.location.api.models.ComponentId
import csw.params.commands.Setup
import csw.params.core.models.Id
import csw.params.events.{Event, EventKey}
import esw.gateway.api.protocol.PostRequest
import esw.gateway.api.protocol.PostRequest.{ComponentCommand, GetEvent, PublishEvent, SequencerCommand}
import esw.gateway.server.handlers.PostHandlerImpl
import esw.http.core.BaseTestSuite
import esw.ocs.api.protocol.SequencerPostRequest.Stop
import org.scalatest.prop.TableDrivenPropertyChecks._

class PostHandlerMetricsTest extends BaseTestSuite {
  private val mockedPostHandler  = mock[PostHandlerImpl]
  private val mockedRoute        = mock[Route]
  private val postHandlerMetrics = new PostHandlerMetrics(mockedPostHandler)

  private def getCounterValue(name: String, labels: Map[String, String]): Double =
    Metrics.prometheusRegistry.getSampleValue(name, labels.keys.toArray, labels.values.toArray)

  private def runCounterTest(postRequest: PostRequest, metricName: String, labelValue: String): Unit = {
    when(mockedPostHandler.handle(postRequest)).thenReturn(mockedRoute)
    def counterValue = getCounterValue(metricName, Map("msg" -> labelValue))

    counterValue shouldBe 0
    (1 to 10).foreach(_ => postHandlerMetrics.handle(postRequest) shouldBe mockedRoute)
    counterValue shouldBe 10
  }

  "Http Metrics" must {
    val command     = mock[Setup]
    val componentId = mock[ComponentId]
    val id          = mock[Id]
    val eventKey    = mock[EventKey]
    val event       = mock[Event]

    Table(
      ("PostRequest", "Label"),
      (ComponentCommand(componentId, Validate(command)), "ComponentCommand_Validate"),
      (ComponentCommand(componentId, Submit(command)), "ComponentCommand_Submit"),
      (ComponentCommand(componentId, Oneway(command)), "ComponentCommand_Oneway"),
      (ComponentCommand(componentId, Query(id)), "ComponentCommand_Query"),
      (GetEvent(Set(eventKey)), "GetEvent"),
      (PublishEvent(event), "PublishEvent"),
      (SequencerCommand(componentId, Stop), "SequencerCommand_Stop")
    ).foreach {
      case (request, label) =>
        s"increment http counter on every $label request | ESW-197" in {
          runCounterTest(request, Metrics.httpCounterMetricName, label)
        }
    }
  }

}
