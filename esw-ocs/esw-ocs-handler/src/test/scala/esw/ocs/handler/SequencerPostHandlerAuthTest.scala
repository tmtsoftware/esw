package esw.ocs.handler

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.BasicDirectives
import akka.http.scaladsl.testkit.ScalatestRouteTest
import csw.aas.http.AuthorizationPolicy.CustomPolicy
import csw.aas.http.SecurityDirectives
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.Id
import csw.prefix.models.{Prefix, Subsystem}
import csw.time.core.models.UTCTime
import esw.ocs.TestHelper.Narrower
import esw.ocs.api.SequencerApi
import esw.ocs.api.codecs.SequencerServiceCodecs
import esw.ocs.api.protocol.SequencerRequest
import esw.ocs.api.protocol.SequencerRequest._
import esw.testcommons.BaseTestSuite
import msocket.api.ContentType
import msocket.http.post.{ClientHttpCodecs, PostRouteFactory}
import msocket.jvm.metrics.LabelExtractor
import msocket.security.models.{Access, AccessToken}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.scalatest.prop.TableDrivenPropertyChecks.forAll
import org.scalatest.prop.Tables.Table
import org.mockito.Mockito.{never, reset, verify, when}

import scala.concurrent.Future

class SequencerPostHandlerAuthTest
    extends BaseTestSuite
    with ScalatestRouteTest
    with SequencerServiceCodecs
    with ClientHttpCodecs {
  override def clientContentType: ContentType = ContentType.Json

  private val componentName        = randomString(10)
  private val subsystem: Subsystem = randomSubsystem
  private val accessToken          = mock[AccessToken]
  private val accessTokenDirective = BasicDirectives.extract(_ => accessToken)
  private val prefix               = Prefix(subsystem, componentName)

  private val sequencer: SequencerApi                = mock[SequencerApi]
  private val securityDirectives: SecurityDirectives = mock[SecurityDirectives]
  private val postHandler = new SequencerPostHandler(sequencer, securityDirectives, Some(prefix)) // started with auth

  import LabelExtractor.Implicits.default
  private val route: Route = new PostRouteFactory[SequencerRequest]("post-endpoint", postHandler).make()

  override protected def beforeEach(): Unit = {
    reset(sequencer)
    reset(securityDirectives)
    reset(accessToken)
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
    val testCasesForAuthEnabledHandlers = Table[SequencerRequest, SequencerApi => Unit](
      ("msg", "api"),
      (LoadSequence(sequence), _.loadSequence(sequence)),
      (StartSequence, _.startSequence()),
      (Submit(sequence), _.submit(sequence)),
      (Add(commands), _.add(commands)),
      (Prepend(commands), _.prepend(commands)),
      (Replace(id, commands), _.replace(id, commands)),
      (InsertAfter(id, commands), _.insertAfter(id, commands)),
      (AddBreakpoint(id), _.addBreakpoint(id)),
      (RemoveBreakpoint(id), _.removeBreakpoint(id)),
      (Reset, _.reset()),
      (Pause, _.pause),
      (Resume, _.resume),
      (GoOnline, _.goOnline()),
      (GoOffline, _.goOffline()),
      (AbortSequence, _.abortSequence()),
      (Stop, _.stop()),
      (DiagnosticMode(startTime, hint), _.diagnosticMode(startTime, hint)),
      (OperationsMode, _.operationsMode())
    )

    forAll(testCasesForAuthEnabledHandlers) { case (msg, api) =>
      val name = msg.getClass.getSimpleName
      s"check for $subsystem subsystem user role policy on $name" in {

        val captor: ArgumentCaptor[CustomPolicy] = ArgumentCaptor.forClass(classOf[CustomPolicy])
        when(securityDirectives.sPost(captor.capture())).thenReturn(accessTokenDirective)
        mockApi(api(sequencer), responseF)

        Post("/post-endpoint", msg.narrow) ~> route ~> check {
          checkSubsystemUserRole(captor)
        }
      }
    }

    val testCasesForAuthDisabledRoutes = Table[SequencerRequest, SequencerApi => Unit](
      ("msg", "api"),
      (GetSequence, _.getSequence),
      (GetSequenceComponent, _.getSequenceComponent),
      (IsAvailable, _.isAvailable),
      (IsOnline, _.isOnline),
      (Query(id), _.query(id))
    )

    forAll(testCasesForAuthDisabledRoutes) { case (msg, api) =>
      val name = msg.getClass.getSimpleName
      s"not check for any AAS policy on $name" in {
        when(securityDirectives.sPost(any[CustomPolicy])).thenReturn(accessTokenDirective)
        mockApi(api(sequencer), responseF)

        Post("/post-endpoint", msg.narrow) ~> route ~> check {
          verify(securityDirectives, never).sPost(any[CustomPolicy]) // should not be called
        }
      }
    }
  }

  def mockApi[T](call: => T, returnValue: T): Unit = when(call).thenReturn(returnValue)

  def checkSubsystemUserRole(captor: ArgumentCaptor[CustomPolicy]): Unit =
    captor.getValue.predicate(AccessToken(realm_access = Access(Set(s"$subsystem-user")))) shouldBe true
}
