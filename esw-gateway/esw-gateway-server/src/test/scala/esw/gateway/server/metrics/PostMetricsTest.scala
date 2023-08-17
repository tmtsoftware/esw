package esw.gateway.server.metrics
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.adapter.ClassicActorSystemOps
import org.apache.pekko.http.scaladsl.marshalling.ToEntityMarshaller
import org.apache.pekko.http.scaladsl.model.HttpRequest
import org.apache.pekko.http.scaladsl.testkit.ScalatestRouteTest
import csw.aas.http.SecurityDirectives
import csw.command.api.messages.CommandServiceRequest.Submit
import csw.command.client.auth.CommandRoles
import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.Assembly
import csw.params.commands.{CommandName, Setup}
import csw.params.core.models.ObsId
import csw.params.events.{EventKey, EventName}
import csw.prefix.models.Prefix
import esw.gateway.api.codecs.GatewayCodecs
import esw.gateway.api.protocol.GatewayRequest
import esw.gateway.api.protocol.GatewayRequest.{ComponentCommand, GetEvent, SequencerCommand}
import esw.gateway.server.handlers.GatewayPostHandler
import esw.gateway.server.{CswTestMocks, GatewayRequestLabels}
import esw.ocs.api.protocol.SequencerRequest.Pause
import esw.testcommons.BaseTestSuite
import io.prometheus.client.CollectorRegistry
import msocket.api.ContentType
import msocket.http.post.headers.{AppNameHeader, UserNameHeader}
import msocket.http.post.{ClientHttpCodecs, PostRouteFactory}
import msocket.jvm.metrics.LabelExtractor
import org.scalatest.prop.Tables.Table

class PostMetricsTest
    extends BaseTestSuite
    with ScalatestRouteTest
    with GatewayCodecs
    with ClientHttpCodecs
    with GatewayRequestLabels {

  override def clientContentType: ContentType = ContentType.Json
  implicit val typedSystem: ActorSystem[_]    = system.toTyped
  private val cswCtxMocks                     = new CswTestMocks()
  import cswCtxMocks.*

  private val securityDirectives = SecurityDirectives.authDisabled(system.settings.config)
  private val commandRoles       = CommandRoles.empty

  private val postHandlerImpl =
    new GatewayPostHandler(alarmApi, resolver, eventApi, loggingApi, adminApi, securityDirectives, commandRoles)
  private val postRoute = new PostRouteFactory[GatewayRequest]("post-endpoint", postHandlerImpl).make(true)
  private val prefix    = Prefix("esw.test")

  private val defaultRegistry = CollectorRegistry.defaultRegistry
  private val command         = Setup(prefix, CommandName("c1"), Some(ObsId("2020A-001-123")))
  private val componentId     = ComponentId(prefix, Assembly)
  private val eventKey        = EventKey(prefix, EventName("event"))

  private def post[E: ToEntityMarshaller](entity: E, appName: String, username: String): HttpRequest =
    Post("/post-endpoint", entity).withHeaders(new AppNameHeader(appName), new UserNameHeader(username))

  private val labelNames = List(
    "msg",
    "app_name",
    "username",
    "command_msg",
    "sequencer_msg"
  )

  private val appName  = randomString(10)
  private val username = randomString(10)
  def labelValues(
      msg: String,
      appName: String = appName,
      username: String = username,
      commandMsg: String = "",
      sequencerMsg: String = ""
  ): List[String] = List(msg, appName, username, commandMsg, sequencerMsg)

  Table(
    ("PostRequest", "Labels"),
    (ComponentCommand(componentId, Submit(command)), labelValues("ComponentCommand", commandMsg = "Submit")),
    (SequencerCommand(componentId, Pause), labelValues("SequencerCommand", sequencerMsg = "Pause")),
    (GetEvent(Set(eventKey)), labelValues("GetEvent"))
  ).foreach { case (request, labels) =>
    s"increment http counter on every ${LabelExtractor.createLabel(request)} request | ESW-197, ESW-386, ESW-531" in {
      runCounterTest(request, labels)
    }
  }

  private def getCounterValue(labelValues: List[String]): Double =
    defaultRegistry.getSampleValue("http_requests_total", labelNames.toArray, labelValues.toArray)

  private def runCounterTest(postRequest: GatewayRequest, labels: List[String]): Unit = {

    def counterValue = getCounterValue(labels)

    counterValue shouldBe 0
    (1 to 1).foreach(_ => post(postRequest, appName, username) ~> postRoute)
    counterValue shouldBe 1
  }

}
