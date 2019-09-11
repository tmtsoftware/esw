package esw.ocs.app.route

import akka.http.scaladsl.testkit.ScalatestRouteTest
import csw.params.core.models.Id
import esw.http.core.BaseTestSuite
import esw.ocs.api.codecs.SequencerAdminHttpCodecs
import esw.ocs.api.models.StepList
import esw.ocs.api.protocol.SequencerAdminPostRequest._
import esw.ocs.api.protocol.EditorError.{CannotOperateOnAnInFlightOrFinishedStep, IdDoesNotExist}
import esw.ocs.api.protocol.{SequencerAdminPostRequest, _}
import esw.ocs.impl.SequencerAdminImpl
import mscoket.impl.HttpCodecs
import org.mockito.Mockito.when

import scala.concurrent.Future

class SequencerAdminRoutesTest extends BaseTestSuite with ScalatestRouteTest with SequencerAdminHttpCodecs with HttpCodecs {

  private val sequencerAdmin: SequencerAdminImpl = mock[SequencerAdminImpl]
  private val postHandler                        = new PostHandlerImpl(sequencerAdmin)
  private val route                              = new SequencerAdminRoutes(postHandler).route

  "SequencerRoutes" must {
    "return sequence for getSequence request | ESW-222" in {
      val stepList = StepList(Id(), List.empty)
      when(sequencerAdmin.getSequence).thenReturn(Future.successful(Some(stepList)))

      Post("/post", GetSequence) ~> route ~> check {
        responseAs[Option[StepList]].get should ===(stepList)
      }
    }

    "return true if sequencer is available for isAvailable request | ESW-222" in {
      when(sequencerAdmin.isAvailable).thenReturn(Future.successful(true))

      Post("/post", IsAvailable) ~> route ~> check {
        responseAs[Boolean] should ===(true)
      }
    }

    "return true if sequencer is online for isOnline request | ESW-222" in {
      when(sequencerAdmin.isOnline).thenReturn(Future.successful(true))

      Post("/post", IsOnline) ~> route ~> check {
        responseAs[Boolean] should ===(true)
      }
    }

    "return Ok for Pause request | ESW-222" in {
      when(sequencerAdmin.pause).thenReturn(Future.successful(Ok))

      Post("/post", Pause) ~> route ~> check {
        responseAs[PauseResponse] should ===(Ok)
      }
    }

    "return CannotOperateOnAnInFlightOrFinishedStep for Pause request | ESW-222" in {
      when(sequencerAdmin.pause).thenReturn(Future.successful(CannotOperateOnAnInFlightOrFinishedStep))

      Post("/post", Pause) ~> route ~> check {
        responseAs[PauseResponse] should ===(CannotOperateOnAnInFlightOrFinishedStep)
      }
    }

    "return Ok for Resume request | ESW-222" in {
      when(sequencerAdmin.resume).thenReturn(Future.successful(Ok))

      Post("/post", Resume) ~> route ~> check {
        responseAs[OkOrUnhandledResponse] should ===(Ok)
      }
    }

    "return Ok for Reset request | ESW-222" in {
      when(sequencerAdmin.reset()).thenReturn(Future.successful(Ok))

      Post("/post", Reset) ~> route ~> check {
        responseAs[OkOrUnhandledResponse] should ===(Ok)
      }
    }

    "return Unhandled for Reset request | ESW-222" in {
      val unhandled = Unhandled("Finished", "reset")
      when(sequencerAdmin.reset()).thenReturn(Future.successful(unhandled))

      Post("/post", Reset) ~> route ~> check {
        responseAs[OkOrUnhandledResponse] should ===(unhandled)
      }
    }

    "return Ok for AbortSequence request | ESW-222" in {
      when(sequencerAdmin.abortSequence()).thenReturn(Future.successful(Ok))

      Post("/post", AbortSequence) ~> route ~> check {
        responseAs[OkOrUnhandledResponse] should ===(Ok)
      }
    }

    "return Ok for GoOnline request | ESW-222" in {
      when(sequencerAdmin.goOnline()).thenReturn(Future.successful(Ok))

      Post("/post", GoOnline) ~> route ~> check {
        responseAs[GoOnlineResponse] should ===(Ok)
      }
    }

    "return GoOnlineHookFailed for GoOnline request | ESW-222" in {
      when(sequencerAdmin.goOnline()).thenReturn(Future.successful(GoOnlineHookFailed))

      Post("/post", GoOnline) ~> route ~> check {
        responseAs[GoOnlineResponse] should ===(GoOnlineHookFailed)
      }
    }

    "return Ok for GoOffline request | ESW-222" in {
      when(sequencerAdmin.goOffline()).thenReturn(Future.successful(Ok))

      Post("/post", GoOffline) ~> route ~> check {
        responseAs[OkOrUnhandledResponse] should ===(Ok)
      }
    }

    "return Ok for Add request | ESW-222" in {
      when(sequencerAdmin.add(List.empty)).thenReturn(Future.successful(Ok))

      Post("/post", Add(List.empty)) ~> route ~> check {
        responseAs[OkOrUnhandledResponse] should ===(Ok)
      }
    }

    "return Ok for Prepend request | ESW-222" in {
      when(sequencerAdmin.prepend(List.empty)).thenReturn(Future.successful(Ok))

      Post("/post", Prepend(List.empty)) ~> route ~> check {
        responseAs[OkOrUnhandledResponse] should ===(Ok)
      }
    }

    "return Ok for Replace request | ESW-222" in {
      val id = Id()
      when(sequencerAdmin.replace(id, List.empty)).thenReturn(Future.successful(Ok))

      Post("/post", Replace(id, List.empty)) ~> route ~> check {
        responseAs[GenericResponse] should ===(Ok)
      }
    }

    "return IdDoesNotExist for Replace request | ESW-222" in {
      val id = Id()
      when(sequencerAdmin.replace(id, List.empty)).thenReturn(Future.successful(IdDoesNotExist(id)))

      Post("/post", Replace(id, List.empty)) ~> route ~> check {
        responseAs[GenericResponse] should ===(IdDoesNotExist(id))
      }
    }

    "return Ok for InsertAfter request | ESW-222" in {
      val id = Id()
      when(sequencerAdmin.insertAfter(id, List.empty)).thenReturn(Future.successful(Ok))

      Post("/post", InsertAfter(id, List.empty)) ~> route ~> check {
        responseAs[GenericResponse] should ===(Ok)
      }
    }

    "return Ok for Delete request | ESW-222" in {
      val id = Id()
      when(sequencerAdmin.delete(id)).thenReturn(Future.successful(Ok))

      Post("/post", SequencerAdminPostRequest.Delete(id)) ~> route ~> check {
        responseAs[GenericResponse] should ===(Ok)
      }
    }

    "return Ok for AddBreakPoint request | ESW-222" in {
      val id = Id()
      when(sequencerAdmin.addBreakpoint(id)).thenReturn(Future.successful(Ok))

      Post("/post", AddBreakpoint(id)) ~> route ~> check {
        responseAs[GenericResponse] should ===(Ok)
      }
    }

    "return Ok for RemoveBreakPoint request | ESW-222" in {
      val id = Id()
      when(sequencerAdmin.removeBreakpoint(id)).thenReturn(Future.successful(Ok))

      Post("/post", RemoveBreakpoint(id)) ~> route ~> check {
        responseAs[GenericResponse] should ===(Ok)
      }
    }
  }

}
