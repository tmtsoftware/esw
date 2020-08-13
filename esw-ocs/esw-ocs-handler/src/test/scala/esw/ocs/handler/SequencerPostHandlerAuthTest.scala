package esw.ocs.handler

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.BasicDirectives
import akka.http.scaladsl.testkit.ScalatestRouteTest
import csw.aas.core.token.AccessToken
import csw.aas.core.token.claims.Access
import csw.aas.http.AuthorizationPolicy.CustomPolicy
import csw.aas.http.SecurityDirectives
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.Id
import csw.prefix.models.{Prefix, Subsystem}
import csw.time.core.models.UTCTime
import esw.ocs.TestHelper.Narrower
import esw.ocs.api.SequencerApi
import esw.ocs.api.codecs.SequencerHttpCodecs
import esw.ocs.api.protocol.SequencerPostRequest
import esw.ocs.api.protocol.SequencerPostRequest._
import esw.testcommons.BaseTestSuite
import msocket.api.ContentType
import msocket.impl.post.{ClientHttpCodecs, PostRouteFactory}
import org.mockito.captor.{ArgCaptor, Captor}
import org.scalatest.prop.TableDrivenPropertyChecks.forAll
import org.scalatest.prop.TableFor3
import org.scalatest.prop.Tables.Table

import scala.concurrent.Future

class SequencerPostHandlerAuthTest extends BaseTestSuite with ScalatestRouteTest with SequencerHttpCodecs with ClientHttpCodecs {
  override def clientContentType: ContentType = ContentType.Json

  private val componentName        = randomString(10)
  private val subsystem: Subsystem = randomSubsystem
  private val accessToken          = mock[AccessToken]
  private val accessTokenDirective = BasicDirectives.extract(_ => accessToken)
  private val prefix               = Prefix(subsystem, componentName)

  private val sequencer: SequencerApi                = mock[SequencerApi]
  private val securityDirectives: SecurityDirectives = mock[SecurityDirectives]
  private val postHandler                            = new SequencerPostHandler(sequencer, securityDirectives, Some(prefix)) // started with auth
  private val route: Route                           = new PostRouteFactory[SequencerPostRequest]("post-endpoint", postHandler).make()

  override protected def beforeEach(): Unit = {
    reset(sequencer, securityDirectives, accessToken)
    super.beforeEach()
  }

  "SequencerPostHandler" must {
    val command   = Setup(Prefix(subsystem, randomString(5)), CommandName("command-1"), None)
    val commands  = List(command)
    val sequence  = Sequence(command)
    val id        = Id()
    val startTime = UTCTime.now()
    val hint      = randomString(5)

    val responseF = Future.failed(new RuntimeException("test")) // common response for all
    val table: TableFor3[String, SequencerPostRequest, SequencerApi => Unit] = Table(
      ("test name", "message", "mocks"),
      ("load sequence", LoadSequence(sequence), { x => mockCall(x.loadSequence(sequence), responseF) }),
      ("start sequence", StartSequence, { x => mockCall(x.startSequence(), responseF) }),
      ("submit action", Submit(sequence), { x => mockCall(x.submit(sequence), responseF) }),
      ("add action", Add(commands), { x => mockCall(x.add(commands), responseF) }),
      ("prepend action", Prepend(commands), { x => mockCall(x.prepend(commands), responseF) }),
      ("Replace action", Replace(id, commands), { x => mockCall(x.replace(id, commands), responseF) }),
      ("insert after action", InsertAfter(id, commands), { x => mockCall(x.insertAfter(id, commands), responseF) }),
      ("add breakpoint action", AddBreakpoint(id), { x => mockCall(x.addBreakpoint(id), responseF) }),
      ("remove breakpoint action", RemoveBreakpoint(id), { x => mockCall(x.removeBreakpoint(id), responseF) }),
      ("reset action", Reset, { x => mockCall(x.reset(), responseF) }),
      ("pause action", Pause, { x => mockCall(x.pause, responseF) }),
      ("resume action", Resume, { x => mockCall(x.resume, responseF) }),
      ("goOnline action", GoOnline, { x => mockCall(x.goOnline(), responseF) }),
      ("goOffline action", GoOffline, { x => mockCall(x.goOffline(), responseF) }),
      ("abortSequence action", AbortSequence, { x => mockCall(x.abortSequence(), responseF) }),
      ("stop action", Stop, { x => mockCall(x.stop(), responseF) }),
      ("diagnostic mode", DiagnosticMode(startTime, hint), { x => mockCall(x.diagnosticMode(startTime, hint), responseF) }),
      ("operations mode", OperationsMode, { x => mockCall(x.operationsMode(), responseF) })
    )

    forAll(table) {
      case (name, msg, mocks) =>
        s"check for $subsystem subsystem user role policy on $name" in {
          val captor = ArgCaptor[CustomPolicy]
          when(securityDirectives.sPost(captor)).thenReturn(accessTokenDirective)
          mocks(sequencer)

          Post("/post-endpoint", msg.narrow) ~> route ~> check {
            checkSubsystemUserRole(captor)
          }
        }
    }
  }

  def mockCall[T](call: => T, returnValue: T): Unit = when(call).thenReturn(returnValue)

  def checkSubsystemUserRole(captor: Captor[CustomPolicy]): Unit =
    captor.value.predicate(AccessToken(realm_access = Access(Set(s"${subsystem}-user")))) shouldBe true
}
