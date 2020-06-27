package esw.sm.handler

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import csw.aas.http.SecurityDirectives
import csw.location.api.models.{ComponentId, ComponentType}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.ocs.api.models.ObsMode
import esw.sm.api.SequenceManagerApi
import esw.sm.api.codecs.SequenceManagerHttpCodec
import esw.sm.api.protocol.SequenceManagerPostRequest._
import esw.sm.api.protocol._
import esw.testcommons.BaseTestSuite
import msocket.api.ContentType
import msocket.impl.post.{ClientHttpCodecs, PostRouteFactory}

import scala.concurrent.Future

class SequenceManagerPostHandlerTest
    extends BaseTestSuite
    with ScalatestRouteTest
    with SequenceManagerHttpCodec
    with ClientHttpCodecs {
  private val sequenceManagerApi: SequenceManagerApi = mock[SequenceManagerApi]
  private val securityDirectives: SecurityDirectives = SecurityDirectives.authDisabled(system.settings.config)
  private val postHandler = new SequenceManagerPostHandler(sequenceManagerApi, securityDirectives)
  lazy val route: Route = new PostRouteFactory[SequenceManagerPostRequest]("post-endpoint", postHandler).make()
  private val obsMode = ObsMode("IRIS_darknight")
  private val componentId = ComponentId(Prefix(ESW, obsMode.name), ComponentType.Sequencer)

  override def clientContentType: ContentType = ContentType.Json

  implicit class Narrower(x: SequenceManagerPostRequest) {
    def narrow: SequenceManagerPostRequest = x
  }

  "SequenceManagerPostHandler" must {
    "return configure success for configure request | ESW-171" in {
      when(sequenceManagerApi.configure(obsMode)).thenReturn(Future.successful(ConfigureResponse.Success(componentId)))

      Post("/post-endpoint", Configure(obsMode).narrow) ~> route ~> check {
        verify(sequenceManagerApi).configure(obsMode)
        responseAs[ConfigureResponse] should ===(ConfigureResponse.Success(componentId))
      }
    }

    "return running observation modes for getRunningObsModes request | ESW-171" in {
      val obsModes = Set(obsMode)
      when(sequenceManagerApi.getRunningObsModes).thenReturn(Future.successful(GetRunningObsModesResponse.Success(obsModes)))

      Post("/post-endpoint", GetRunningObsModes.narrow) ~> route ~> check {
        verify(sequenceManagerApi).getRunningObsModes
        responseAs[GetRunningObsModesResponse] should ===(GetRunningObsModesResponse.Success(obsModes))
      }
    }

    "return start sequencer success for startSequencer request | ESW-171" in {
      when(sequenceManagerApi.startSequencer(ESW, obsMode))
        .thenReturn(Future.successful(StartSequencerResponse.Started(componentId)))

      Post("/post-endpoint", StartSequencer(ESW, obsMode).narrow) ~> route ~> check {
        verify(sequenceManagerApi).startSequencer(ESW, obsMode)
        responseAs[StartSequencerResponse] should ===(StartSequencerResponse.Started(componentId))
      }
    }

    "return shutdown sequencer success for shutdownSequencers request | ESW-171" in {
      when(sequenceManagerApi.shutdownSequencers(Some(ESW), Some(obsMode)))
        .thenReturn(Future.successful(ShutdownSequencersResponse.Success))

      Post(
        "/post-endpoint",
        ShutdownSequencers(Some(ESW), Some(obsMode), shutdownSequenceComp = false).narrow
      ) ~> route ~> check {
        verify(sequenceManagerApi).shutdownSequencers(Some(ESW), Some(obsMode))
        responseAs[ShutdownSequencersResponse] should ===(ShutdownSequencersResponse.Success)
      }
    }

    "return restart sequencer success for restartSequencer request | ESW-171" in {
      when(sequenceManagerApi.restartSequencer(ESW, obsMode))
        .thenReturn(Future.successful(RestartSequencerResponse.Success(componentId)))

      Post("/post-endpoint", RestartSequencer(ESW, obsMode).narrow) ~> route ~> check {
        verify(sequenceManagerApi).restartSequencer(ESW, obsMode)
        responseAs[RestartSequencerResponse] should ===(RestartSequencerResponse.Success(componentId))
      }
    }

    "return spawn sequence component success for spawnSequenceComponent request | ESW-337" in {
      val seqCompName = "seq_comp"
      val machine = ComponentId(Prefix(ESW, "primary"), ComponentType.Machine)
      val seqComp = ComponentId(Prefix(ESW, seqCompName), ComponentType.SequenceComponent)

      when(sequenceManagerApi.spawnSequenceComponent(machine, seqCompName))
        .thenReturn(Future.successful(SpawnSequenceComponentResponse.Success(seqComp)))

      Post("/post-endpoint", SpawnSequenceComponent(machine, seqCompName).narrow) ~> route ~> check {
        verify(sequenceManagerApi).spawnSequenceComponent(machine, seqCompName)
        responseAs[SpawnSequenceComponentResponse] should ===(SpawnSequenceComponentResponse.Success(seqComp))

        "return shutdown all sequencer success for shutdownSequencers request for all sequencers | ESW-171" in {
          when(sequenceManagerApi.shutdownSequencers(None, None))
            .thenReturn(Future.successful(ShutdownSequencersResponse.Success))

          Post("/post-endpoint", ShutdownSequencers(None, None, shutdownSequenceComp = false).narrow) ~> route ~> check {
            verify(sequenceManagerApi).shutdownSequencers(None, None)
            responseAs[ShutdownSequencersResponse] should ===(ShutdownSequencersResponse.Success)
          }
        }
      }
    }
  }
}