package esw.ocs.app

import akka.http.scaladsl.testkit.ScalatestRouteTest
import csw.params.core.models.Id
import esw.http.core.BaseTestSuite
import esw.ocs.api.models.StepList
import esw.ocs.api.models.codecs.SequencerAdminHttpCodecs
import esw.ocs.api.models.request.SequencerAdminPostRequest.GetSequence
import esw.ocs.impl.SequencerAdminImpl
import mscoket.impl.HttpCodecs
import org.mockito.Mockito.when

import scala.concurrent.Future

class SequencerAdminRoutesTest extends BaseTestSuite with ScalatestRouteTest with SequencerAdminHttpCodecs with HttpCodecs {

  private val sequencerAdminClient: SequencerAdminImpl = mock[SequencerAdminImpl]
  private val postHandler                              = new PostHandlerImpl(sequencerAdminClient)
  private val route                                    = new SequencerAdminRoutes(postHandler).route

  "SequencerRoutes" must {
    "return sequence for getSequence request | ESW-222" in {
      val stepList = StepList(Id(), List.empty)
      when(sequencerAdminClient.getSequence).thenReturn(Future.successful(Some(stepList)))

      Post("/post", GetSequence) ~> route ~> check {
        responseAs[Option[StepList]].get should ===(stepList)
      }
    }
  }

}
