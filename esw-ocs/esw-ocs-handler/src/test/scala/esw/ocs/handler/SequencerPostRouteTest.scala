package esw.ocs.handler

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import csw.aas.http.SecurityDirectives
import csw.params.commands.CommandIssue.IdNotAvailableIssue
import csw.params.commands.CommandResponse._
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.Id
import csw.prefix.models.Prefix
import csw.time.core.models.UTCTime
import esw.ocs.api.codecs.SequencerHttpCodecs
import esw.ocs.api.models.StepList
import esw.ocs.api.protocol.EditorError.{CannotOperateOnAnInFlightOrFinishedStep, IdDoesNotExist}
import esw.ocs.api.protocol.SequencerPostRequest._
import esw.ocs.api.protocol._
import esw.ocs.api.SequencerApi
import msocket.api.ContentType
import msocket.impl.post.{ClientHttpCodecs, PostRouteFactory}
import esw.commons.BaseTestSuite
import scala.concurrent.Future

class SequencerPostRouteTest extends BaseTestSuite with ScalatestRouteTest with SequencerHttpCodecs with ClientHttpCodecs {

  private val sequencer: SequencerApi                = mock[SequencerApi]
  private val securityDirectives: SecurityDirectives = SecurityDirectives.authDisabled(system.settings.config)
  private val postHandler                            = new SequencerPostHandler(sequencer, securityDirectives)
  lazy val route: Route                              = new PostRouteFactory[SequencerPostRequest]("post-endpoint", postHandler).make()

  override def clientContentType: ContentType = ContentType.Json

  override def afterEach(): Unit = {
    reset(sequencer)
  }

  implicit class Narrower(x: SequencerPostRequest) {
    def narrow: SequencerPostRequest = x
  }

  "SequencerRoutes" must {
    "return sequence for getSequence request | ESW-222" in {
      val stepList = StepList(List.empty)
      when(sequencer.getSequence).thenReturn(Future.successful(Some(stepList)))

      Post("/post-endpoint", GetSequence.narrow) ~> route ~> check {
        verify(sequencer).getSequence
        responseAs[Option[StepList]].get should ===(stepList)
      }
    }

    "return true if sequencer is available for isAvailable request | ESW-222" in {
      when(sequencer.isAvailable).thenReturn(Future.successful(true))

      Post("/post-endpoint", IsAvailable.narrow) ~> route ~> check {
        verify(sequencer).isAvailable
        responseAs[Boolean] should ===(true)
      }
    }

    "return true if sequencer is online for isOnline request | ESW-222" in {
      when(sequencer.isOnline).thenReturn(Future.successful(true))

      Post("/post-endpoint", IsOnline.narrow) ~> route ~> check {
        verify(sequencer).isOnline
        responseAs[Boolean] should ===(true)
      }
    }

    "return Ok for Pause request | ESW-222" in {
      when(sequencer.pause).thenReturn(Future.successful(Ok))

      Post("/post-endpoint", Pause.narrow) ~> route ~> check {
        verify(sequencer).pause
        responseAs[PauseResponse] should ===(Ok)
      }
    }

    "return CannotOperateOnAnInFlightOrFinishedStep for Pause request | ESW-222" in {
      when(sequencer.pause).thenReturn(Future.successful(CannotOperateOnAnInFlightOrFinishedStep))

      Post("/post-endpoint", Pause.narrow) ~> route ~> check {
        verify(sequencer).pause
        responseAs[PauseResponse] should ===(CannotOperateOnAnInFlightOrFinishedStep)
      }
    }

    "return Ok for Resume request | ESW-222" in {
      when(sequencer.resume).thenReturn(Future.successful(Ok))

      Post("/post-endpoint", Resume.narrow) ~> route ~> check {
        verify(sequencer).resume
        responseAs[OkOrUnhandledResponse] should ===(Ok)
      }
    }

    "return Ok for Reset request | ESW-222" in {
      when(sequencer.reset()).thenReturn(Future.successful(Ok))

      Post("/post-endpoint", Reset.narrow) ~> route ~> check {
        verify(sequencer).reset()
        responseAs[OkOrUnhandledResponse] should ===(Ok)
      }
    }

    "return Unhandled for Reset request | ESW-222" in {
      val unhandled = Unhandled("Finished", "reset")
      when(sequencer.reset()).thenReturn(Future.successful(unhandled))

      Post("/post-endpoint", Reset.narrow) ~> route ~> check {
        verify(sequencer).reset()
        responseAs[OkOrUnhandledResponse] should ===(unhandled)
      }
    }

    "return Ok for AbortSequence request | ESW-222" in {
      when(sequencer.abortSequence()).thenReturn(Future.successful(Ok))

      Post("/post-endpoint", AbortSequence.narrow) ~> route ~> check {
        verify(sequencer).abortSequence()
        responseAs[OkOrUnhandledResponse] should ===(Ok)
      }
    }

    "return Ok for Stop request | ESW-222" in {
      when(sequencer.stop()).thenReturn(Future.successful(Ok))

      Post("/post-endpoint", Stop.narrow) ~> route ~> check {
        verify(sequencer).stop()
        responseAs[OkOrUnhandledResponse] should ===(Ok)
      }
    }

    "return Ok for GoOnline request | ESW-222" in {
      when(sequencer.goOnline()).thenReturn(Future.successful(Ok))

      Post("/post-endpoint", GoOnline.narrow) ~> route ~> check {
        verify(sequencer).goOnline()
        responseAs[GoOnlineResponse] should ===(Ok)
      }
    }

    "return GoOnlineHookFailed for GoOnline request | ESW-222" in {
      when(sequencer.goOnline()).thenReturn(Future.successful(GoOnlineHookFailed()))

      Post("/post-endpoint", GoOnline.narrow) ~> route ~> check {
        verify(sequencer).goOnline()
        responseAs[GoOnlineResponse] should ===(GoOnlineHookFailed())
      }
    }

    "return Ok for GoOffline request | ESW-222" in {
      when(sequencer.goOffline()).thenReturn(Future.successful(Ok))

      Post("/post-endpoint", GoOffline.narrow) ~> route ~> check {
        verify(sequencer).goOffline()
        responseAs[OkOrUnhandledResponse] should ===(Ok)
      }
    }

    "return Ok for Add request | ESW-222" in {
      when(sequencer.add(List.empty)).thenReturn(Future.successful(Ok))

      Post("/post-endpoint", Add(List.empty).narrow) ~> route ~> check {
        verify(sequencer).add(List.empty)
        responseAs[OkOrUnhandledResponse] should ===(Ok)
      }
    }

    "return Ok for Prepend request | ESW-222" in {
      when(sequencer.prepend(List.empty)).thenReturn(Future.successful(Ok))

      Post("/post-endpoint", Prepend(List.empty).narrow) ~> route ~> check {
        verify(sequencer).prepend(List.empty)
        responseAs[OkOrUnhandledResponse] should ===(Ok)
      }
    }

    "return Ok for Replace request | ESW-222" in {
      val id = Id()
      when(sequencer.replace(id, List.empty)).thenReturn(Future.successful(Ok))

      Post("/post-endpoint", Replace(id, List.empty).narrow) ~> route ~> check {
        verify(sequencer).replace(id, List.empty)
        responseAs[GenericResponse] should ===(Ok)
      }
    }

    "return IdDoesNotExist for Replace request | ESW-222" in {
      val id = Id()
      when(sequencer.replace(id, List.empty)).thenReturn(Future.successful(IdDoesNotExist(id)))

      Post("/post-endpoint", Replace(id, List.empty).narrow) ~> route ~> check {
        verify(sequencer).replace(id, List.empty)
        responseAs[GenericResponse] should ===(IdDoesNotExist(id))
      }
    }

    "return Ok for InsertAfter request | ESW-222" in {
      val id = Id()
      when(sequencer.insertAfter(id, List.empty)).thenReturn(Future.successful(Ok))

      Post("/post-endpoint", InsertAfter(id, List.empty).narrow) ~> route ~> check {
        verify(sequencer).insertAfter(id, List.empty)
        responseAs[GenericResponse] should ===(Ok)
      }
    }

    "return Ok for Delete request | ESW-222" in {
      val id = Id()
      when(sequencer.delete(id)).thenReturn(Future.successful(Ok))

      Post("/post-endpoint", SequencerPostRequest.Delete(id).narrow) ~> route ~> check {
        verify(sequencer).delete(id)
        responseAs[GenericResponse] should ===(Ok)
      }
    }

    "return Ok for AddBreakPoint request | ESW-222" in {
      val id = Id()
      when(sequencer.addBreakpoint(id)).thenReturn(Future.successful(Ok))

      Post("/post-endpoint", AddBreakpoint(id).narrow) ~> route ~> check {
        verify(sequencer).addBreakpoint(id)
        responseAs[GenericResponse] should ===(Ok)
      }
    }

    "return Ok for RemoveBreakPoint request | ESW-222" in {
      val id = Id()
      when(sequencer.removeBreakpoint(id)).thenReturn(Future.successful(Ok))

      Post("/post-endpoint", RemoveBreakpoint(id).narrow) ~> route ~> check {
        verify(sequencer).removeBreakpoint(id)
        responseAs[GenericResponse] should ===(Ok)
      }
    }

    "return Ok for LoadSequence request | ESW-101" in {
      val command1 = Setup(Prefix("esw.test"), CommandName("command-1"), None)
      val sequence = Sequence(command1)
      when(sequencer.loadSequence(sequence)).thenReturn(Future.successful(Ok))

      Post("/post-endpoint", LoadSequence(sequence).narrow) ~> route ~> check {
        verify(sequencer).loadSequence(sequence)
        responseAs[OkOrUnhandledResponse] should ===(Ok)
      }
    }

    "return Started response for StartSequence request | ESW-101" in {
      val startedResponse = Started(Id())
      when(sequencer.startSequence()).thenReturn(Future.successful(startedResponse))

      Post("/post-endpoint", StartSequence.narrow) ~> route ~> check {
        verify(sequencer).startSequence()
        responseAs[SubmitResponse] should ===(startedResponse)
      }
    }

    "return SubmitResponse for Submit request | ESW-101" in {
      val command1          = Setup(Prefix("esw.test"), CommandName("command-1"), None)
      val sequence          = Sequence(command1)
      val completedResponse = Completed(Id())
      when(sequencer.submit(sequence)).thenReturn(Future.successful(completedResponse))

      Post("/post-endpoint", Submit(sequence).narrow) ~> route ~> check {
        verify(sequencer).submit(sequence)
        responseAs[SubmitResponse] should ===(completedResponse)
      }
    }

    "return SubmitResponse for Query request | ESW-101, ESW-244" in {
      val sequenceId = Id()
      val completedResponse =
        Invalid(sequenceId, IdNotAvailableIssue(s"Sequencer is not running any sequence with runId $sequenceId"))
      when(sequencer.query(sequenceId)).thenReturn(Future.successful(completedResponse))

      Post("/post-endpoint", Query(sequenceId).narrow) ~> route ~> check {
        verify(sequencer).query(sequenceId)
        responseAs[SubmitResponse] should ===(completedResponse)
      }
    }

    "return Ok for DiagnosticMode request | ESW-143" in {
      val startTime = UTCTime.now()
      val hint      = "engineering"
      when(sequencer.diagnosticMode(startTime, hint)).thenReturn(Future.successful(Ok))

      Post("/post-endpoint", DiagnosticMode(startTime, hint).narrow) ~> route ~> check {
        verify(sequencer).diagnosticMode(startTime, hint)
        responseAs[DiagnosticModeResponse] should ===(Ok)
      }
    }

    "return Ok for OperationsMode request | ESW-143" in {
      when(sequencer.operationsMode()).thenReturn(Future.successful(Ok))

      Post("/post-endpoint", OperationsMode.narrow) ~> route ~> check {
        verify(sequencer).operationsMode()
        responseAs[OperationsModeResponse] should ===(Ok)
      }
    }

    "show internal server error when there is an exception at server side" in {
      when(sequencer.getSequence).thenReturn(Future.failed(new RuntimeException("test")))

      Post("/post-endpoint", GetSequence.narrow) ~> route ~> check {
        verify(sequencer, atLeast(1)).getSequence
        status should ===(StatusCodes.InternalServerError)
      }
    }
  }

}
