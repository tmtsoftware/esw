package esw.ocs.app.route

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.{Id, Prefix}
import csw.time.core.models.UTCTime
import esw.http.core.BaseTestSuite
import esw.ocs.api.codecs.SequencerAdminHttpCodecs
import esw.ocs.api.models.StepList
import esw.ocs.api.protocol.EditorError.{CannotOperateOnAnInFlightOrFinishedStep, IdDoesNotExist}
import esw.ocs.api.protocol.SequencerAdminPostRequest._
import esw.ocs.api.protocol.{SequencerAdminPostRequest, _}
import esw.ocs.impl.SequencerAdminImpl
import mscoket.impl.HttpCodecs
import org.mockito.Mockito.when

import scala.concurrent.Future

class SequencerAdminPostRouteTest extends BaseTestSuite with ScalatestRouteTest with SequencerAdminHttpCodecs with HttpCodecs {

  private val sequencerAdmin: SequencerAdminImpl = mock[SequencerAdminImpl]
  private val postHandler                        = new PostHandlerImpl(sequencerAdmin)
  private val websocketHandler                   = new WebsocketHandlerImpl(sequencerAdmin)
  private val route                              = new SequencerAdminRoutes(postHandler, websocketHandler).route

  "SequencerRoutes" must {
    "return sequence for getSequence request | ESW-222" in {
      val stepList = StepList(Id(), List.empty)
      when(sequencerAdmin.getSequence).thenReturn(Future.successful(Some(stepList)))

      Post("/post-endpoint", GetSequence) ~> route ~> check {
        responseAs[Option[StepList]].get should ===(stepList)
      }
    }

    "return true if sequencer is available for isAvailable request | ESW-222" in {
      when(sequencerAdmin.isAvailable).thenReturn(Future.successful(true))

      Post("/post-endpoint", IsAvailable) ~> route ~> check {
        responseAs[Boolean] should ===(true)
      }
    }

    "return true if sequencer is online for isOnline request | ESW-222" in {
      when(sequencerAdmin.isOnline).thenReturn(Future.successful(true))

      Post("/post-endpoint", IsOnline) ~> route ~> check {
        responseAs[Boolean] should ===(true)
      }
    }

    "return Ok for Pause request | ESW-222" in {
      when(sequencerAdmin.pause).thenReturn(Future.successful(Ok))

      Post("/post-endpoint", Pause) ~> route ~> check {
        responseAs[PauseResponse] should ===(Ok)
      }
    }

    "return CannotOperateOnAnInFlightOrFinishedStep for Pause request | ESW-222" in {
      when(sequencerAdmin.pause).thenReturn(Future.successful(CannotOperateOnAnInFlightOrFinishedStep))

      Post("/post-endpoint", Pause) ~> route ~> check {
        responseAs[PauseResponse] should ===(CannotOperateOnAnInFlightOrFinishedStep)
      }
    }

    "return Ok for Resume request | ESW-222" in {
      when(sequencerAdmin.resume).thenReturn(Future.successful(Ok))

      Post("/post-endpoint", Resume) ~> route ~> check {
        responseAs[OkOrUnhandledResponse] should ===(Ok)
      }
    }

    "return Ok for Reset request | ESW-222" in {
      when(sequencerAdmin.reset()).thenReturn(Future.successful(Ok))

      Post("/post-endpoint", Reset) ~> route ~> check {
        responseAs[OkOrUnhandledResponse] should ===(Ok)
      }
    }

    "return Unhandled for Reset request | ESW-222" in {
      val unhandled = Unhandled("Finished", "reset")
      when(sequencerAdmin.reset()).thenReturn(Future.successful(unhandled))

      Post("/post-endpoint", Reset) ~> route ~> check {
        responseAs[OkOrUnhandledResponse] should ===(unhandled)
      }
    }

    "return Ok for AbortSequence request | ESW-222" in {
      when(sequencerAdmin.abortSequence()).thenReturn(Future.successful(Ok))

      Post("/post-endpoint", AbortSequence) ~> route ~> check {
        responseAs[OkOrUnhandledResponse] should ===(Ok)
      }
    }

    "return Ok for GoOnline request | ESW-222" in {
      when(sequencerAdmin.goOnline()).thenReturn(Future.successful(Ok))

      Post("/post-endpoint", GoOnline) ~> route ~> check {
        responseAs[GoOnlineResponse] should ===(Ok)
      }
    }

    "return GoOnlineHookFailed for GoOnline request | ESW-222" in {
      when(sequencerAdmin.goOnline()).thenReturn(Future.successful(GoOnlineHookFailed))

      Post("/post-endpoint", GoOnline) ~> route ~> check {
        responseAs[GoOnlineResponse] should ===(GoOnlineHookFailed)
      }
    }

    "return Ok for GoOffline request | ESW-222" in {
      when(sequencerAdmin.goOffline()).thenReturn(Future.successful(Ok))

      Post("/post-endpoint", GoOffline) ~> route ~> check {
        responseAs[OkOrUnhandledResponse] should ===(Ok)
      }
    }

    "return Ok for Add request | ESW-222" in {
      when(sequencerAdmin.add(List.empty)).thenReturn(Future.successful(Ok))

      Post("/post-endpoint", Add(List.empty)) ~> route ~> check {
        responseAs[OkOrUnhandledResponse] should ===(Ok)
      }
    }

    "return Ok for Prepend request | ESW-222" in {
      when(sequencerAdmin.prepend(List.empty)).thenReturn(Future.successful(Ok))

      Post("/post-endpoint", Prepend(List.empty)) ~> route ~> check {
        responseAs[OkOrUnhandledResponse] should ===(Ok)
      }
    }

    "return Ok for Replace request | ESW-222" in {
      val id = Id()
      when(sequencerAdmin.replace(id, List.empty)).thenReturn(Future.successful(Ok))

      Post("/post-endpoint", Replace(id, List.empty)) ~> route ~> check {
        responseAs[GenericResponse] should ===(Ok)
      }
    }

    "return IdDoesNotExist for Replace request | ESW-222" in {
      val id = Id()
      when(sequencerAdmin.replace(id, List.empty)).thenReturn(Future.successful(IdDoesNotExist(id)))

      Post("/post-endpoint", Replace(id, List.empty)) ~> route ~> check {
        responseAs[GenericResponse] should ===(IdDoesNotExist(id))
      }
    }

    "return Ok for InsertAfter request | ESW-222" in {
      val id = Id()
      when(sequencerAdmin.insertAfter(id, List.empty)).thenReturn(Future.successful(Ok))

      Post("/post-endpoint", InsertAfter(id, List.empty)) ~> route ~> check {
        responseAs[GenericResponse] should ===(Ok)
      }
    }

    "return Ok for Delete request | ESW-222" in {
      val id = Id()
      when(sequencerAdmin.delete(id)).thenReturn(Future.successful(Ok))

      Post("/post-endpoint", SequencerAdminPostRequest.Delete(id)) ~> route ~> check {
        responseAs[GenericResponse] should ===(Ok)
      }
    }

    "return Ok for AddBreakPoint request | ESW-222" in {
      val id = Id()
      when(sequencerAdmin.addBreakpoint(id)).thenReturn(Future.successful(Ok))

      Post("/post-endpoint", AddBreakpoint(id)) ~> route ~> check {
        responseAs[GenericResponse] should ===(Ok)
      }
    }

    "return Ok for RemoveBreakPoint request | ESW-222" in {
      val id = Id()
      when(sequencerAdmin.removeBreakpoint(id)).thenReturn(Future.successful(Ok))

      Post("/post-endpoint", RemoveBreakpoint(id)) ~> route ~> check {
        responseAs[GenericResponse] should ===(Ok)
      }
    }

    "return Ok for LoadSequence request | ESW-101" in {
      val command1 = Setup(Prefix("esw.test"), CommandName("command-1"), None)
      val sequence = Sequence(command1)
      when(sequencerAdmin.loadSequence(sequence)).thenReturn(Future.successful(Ok))

      Post("/post-endpoint", LoadSequence(sequence)) ~> route ~> check {
        responseAs[LoadSequenceResponse] should ===(Ok)
      }
    }

    "return Ok for StartSequence request | ESW-101" in {
      when(sequencerAdmin.startSequence).thenReturn(Future.successful(Ok))

      Post("/post-endpoint", StartSequence) ~> route ~> check {
        responseAs[OkOrUnhandledResponse] should ===(Ok)
      }
    }

    "return Ok for LoadAndStartSequence request | ESW-101" in {
      val command1 = Setup(Prefix("esw.test"), CommandName("command-1"), None)
      val sequence = Sequence(command1)
      when(sequencerAdmin.submitSequence(sequence)).thenReturn(Future.successful(Ok))

      Post("/post-endpoint", SubmitSequence(sequence)) ~> route ~> check {
        responseAs[LoadSequenceResponse] should ===(Ok)
      }
    }

    "return Ok for DiagnosticMode request | ESW-143" in {
      val startTime = UTCTime.now()
      val hint      = "engineering"
      when(sequencerAdmin.diagnosticMode(startTime, hint)).thenReturn(Future.successful(Ok))

      Post("/post-endpoint", DiagnosticMode(startTime, hint)) ~> route ~> check {
        responseAs[DiagnosticModeResponse] should ===(Ok)
      }
    }

    "return Ok for OperationsMode request | ESW-143" in {
      when(sequencerAdmin.operationsMode()).thenReturn(Future.successful(Ok))

      Post("/post-endpoint", OperationsMode) ~> route ~> check {
        responseAs[OperationsModeResponse] should ===(Ok)
      }
    }

    "show internal server error when there is an exception at server side" in {
      when(sequencerAdmin.getSequence).thenReturn(Future.failed(new RuntimeException("test")))

      Post("/post-endpoint", GetSequence) ~> route ~> check {
        status should ===(StatusCodes.InternalServerError)
      }
    }
  }

}
