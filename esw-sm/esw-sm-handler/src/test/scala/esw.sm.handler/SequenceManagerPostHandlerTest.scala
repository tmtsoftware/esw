package esw.sm.handler

import akka.http.scaladsl.server.directives.BasicDirectives
import akka.http.scaladsl.testkit.ScalatestRouteTest
import csw.aas.core.token.AccessToken
import csw.aas.http.SecurityDirectives
import csw.location.api.models.{ComponentId, ComponentType}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.ocs.api.models.ObsMode
import esw.sm.api.SequenceManagerApi
import esw.sm.api.codecs.SequenceManagerHttpCodec
import esw.sm.api.models.AgentStatusResponses.AgentSeqCompsStatus
import esw.sm.api.models.ProvisionConfig
import esw.sm.api.protocol.SequenceManagerPostRequest._
import esw.sm.api.protocol._
import esw.sm.auth.EswUserRolePolicy
import esw.testcommons.BaseTestSuite
import msocket.api.ContentType
import msocket.impl.post.{ClientHttpCodecs, PostRouteFactory}

import scala.concurrent.Future
import scala.util.Random

class SequenceManagerPostHandlerTest
    extends BaseTestSuite
    with ScalatestRouteTest
    with SequenceManagerHttpCodec
    with ClientHttpCodecs {
  private val sequenceManagerApi = mock[SequenceManagerApi]
  private val securityDirectives = mock[SecurityDirectives]
  private val postHandler        = new SequenceManagerPostHandler(sequenceManagerApi, securityDirectives)
  private val route              = new PostRouteFactory[SequenceManagerPostRequest]("post-endpoint", postHandler).make()

  private val string10    = Random.alphanumeric.take(10).mkString
  private val obsMode     = ObsMode(string10)
  private val componentId = ComponentId(Prefix(ESW, obsMode.name), ComponentType.Sequencer)

  private val eswUserPolicy        = EswUserRolePolicy()
  private val accessToken          = mock[AccessToken]
  private val accessTokenDirective = BasicDirectives.extract(_ => accessToken)

  override def clientContentType: ContentType = ContentType.Json

  override protected def afterEach(): Unit = {
    super.afterEach()
    reset(securityDirectives, sequenceManagerApi)
  }

  implicit class Narrower(x: SequenceManagerPostRequest) {
    def narrow: SequenceManagerPostRequest = x
  }

  "SequenceManagerPostHandler" must {
    "return configure success for configure request | ESW-171" in {
      when(securityDirectives.sPost(eswUserPolicy)).thenReturn(accessTokenDirective)
      when(sequenceManagerApi.configure(obsMode)).thenReturn(Future.successful(ConfigureResponse.Success(componentId)))

      Post("/post-endpoint", Configure(obsMode).narrow) ~> route ~> check {
        verify(securityDirectives).sPost(eswUserPolicy)
        verify(sequenceManagerApi).configure(obsMode)
        responseAs[ConfigureResponse] should ===(ConfigureResponse.Success(componentId))
      }
    }

    "return provision success for provision request | ESW-347" in {
      val provisionConfig = ProvisionConfig(Prefix(ESW, "primary") -> 1)
      when(securityDirectives.sPost(eswUserPolicy)).thenReturn(accessTokenDirective)
      when(sequenceManagerApi.provision(provisionConfig)).thenReturn(Future.successful(ProvisionResponse.Success))

      Post("/post-endpoint", Provision(provisionConfig).narrow) ~> route ~> check {
        verify(securityDirectives).sPost(eswUserPolicy)
        verify(sequenceManagerApi).provision(provisionConfig)
        responseAs[ProvisionResponse] should ===(ProvisionResponse.Success)
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
      when(securityDirectives.sPost(eswUserPolicy)).thenReturn(accessTokenDirective)
      when(sequenceManagerApi.startSequencer(ESW, obsMode))
        .thenReturn(Future.successful(StartSequencerResponse.Started(componentId)))

      Post("/post-endpoint", StartSequencer(ESW, obsMode).narrow) ~> route ~> check {
        verify(sequenceManagerApi).startSequencer(ESW, obsMode)
        verify(securityDirectives).sPost(eswUserPolicy)
        responseAs[StartSequencerResponse] should ===(StartSequencerResponse.Started(componentId))
      }
    }

    "return shutdown sequencer success for shutdownSequencer request | ESW-171" in {
      when(securityDirectives.sPost(eswUserPolicy)).thenReturn(accessTokenDirective)
      when(sequenceManagerApi.shutdownSequencer(ESW, obsMode)).thenReturn(Future.successful(ShutdownSequencersResponse.Success))

      Post("/post-endpoint", ShutdownSequencer(ESW, obsMode).narrow) ~> route ~> check {
        verify(securityDirectives).sPost(eswUserPolicy)
        verify(sequenceManagerApi).shutdownSequencer(ESW, obsMode)
        responseAs[ShutdownSequencersResponse] should ===(ShutdownSequencersResponse.Success)
      }
    }

    "return success for shutdownSubsystemSequencers request | ESW-171" in {
      when(securityDirectives.sPost(eswUserPolicy)).thenReturn(accessTokenDirective)
      when(sequenceManagerApi.shutdownSubsystemSequencers(ESW)).thenReturn(Future.successful(ShutdownSequencersResponse.Success))

      Post("/post-endpoint", ShutdownSubsystemSequencers(ESW).narrow) ~> route ~> check {
        verify(securityDirectives).sPost(eswUserPolicy)
        verify(sequenceManagerApi).shutdownSubsystemSequencers(ESW)
        responseAs[ShutdownSequencersResponse] should ===(ShutdownSequencersResponse.Success)
      }
    }

    "return success for shutdownObsModeSequencers request | ESW-171" in {
      when(securityDirectives.sPost(eswUserPolicy)).thenReturn(accessTokenDirective)
      when(sequenceManagerApi.shutdownObsModeSequencers(obsMode))
        .thenReturn(Future.successful(ShutdownSequencersResponse.Success))

      Post("/post-endpoint", ShutdownObsModeSequencers(obsMode).narrow) ~> route ~> check {
        verify(securityDirectives).sPost(eswUserPolicy)
        verify(sequenceManagerApi).shutdownObsModeSequencers(obsMode)
        responseAs[ShutdownSequencersResponse] should ===(ShutdownSequencersResponse.Success)
      }
    }

    "return shutdown all sequencer success for shutdownAllSequencer request | ESW-171" in {
      when(securityDirectives.sPost(eswUserPolicy)).thenReturn(accessTokenDirective)
      when(sequenceManagerApi.shutdownAllSequencers()).thenReturn(Future.successful(ShutdownSequencersResponse.Success))

      Post("/post-endpoint", ShutdownAllSequencers.narrow) ~> route ~> check {
        verify(securityDirectives).sPost(eswUserPolicy)
        verify(sequenceManagerApi).shutdownAllSequencers()
        responseAs[ShutdownSequencersResponse] should ===(ShutdownSequencersResponse.Success)
      }
    }

    "return restart sequencer success for restartSequencer request | ESW-171" in {
      when(securityDirectives.sPost(eswUserPolicy)).thenReturn(accessTokenDirective)
      when(sequenceManagerApi.restartSequencer(ESW, obsMode))
        .thenReturn(Future.successful(RestartSequencerResponse.Success(componentId)))

      Post("/post-endpoint", RestartSequencer(ESW, obsMode).narrow) ~> route ~> check {
        verify(securityDirectives).sPost(eswUserPolicy)
        verify(sequenceManagerApi).restartSequencer(ESW, obsMode)
        responseAs[RestartSequencerResponse] should ===(RestartSequencerResponse.Success(componentId))
      }
    }

    "return shutdown sequence component success for shutdownSequenceComponent request | ESW-338" in {
      val prefix = Prefix(ESW, obsMode.name)
      when(securityDirectives.sPost(eswUserPolicy)).thenReturn(accessTokenDirective)
      when(sequenceManagerApi.shutdownSequenceComponent(prefix))
        .thenReturn(Future.successful(ShutdownSequenceComponentResponse.Success))

      Post("/post-endpoint", ShutdownSequenceComponent(prefix).narrow) ~> route ~> check {
        verify(securityDirectives).sPost(eswUserPolicy)
        verify(sequenceManagerApi).shutdownSequenceComponent(prefix)
        responseAs[ShutdownSequenceComponentResponse] should ===(ShutdownSequenceComponentResponse.Success)
      }
    }

    "return shutdown sequence component success for shutdownAllSequenceComponent request | ESW-346" in {
      when(securityDirectives.sPost(eswUserPolicy)).thenReturn(accessTokenDirective)
      when(sequenceManagerApi.shutdownAllSequenceComponents())
        .thenReturn(Future.successful(ShutdownSequenceComponentResponse.Success))

      Post("/post-endpoint", ShutdownAllSequenceComponents.narrow) ~> route ~> check {
        verify(securityDirectives).sPost(eswUserPolicy)
        verify(sequenceManagerApi).shutdownAllSequenceComponents()
        responseAs[ShutdownSequenceComponentResponse] should ===(ShutdownSequenceComponentResponse.Success)
      }
    }

    "return spawn sequence component success for spawnSequenceComponent request | ESW-337" in {
      val seqCompName = "seq_comp"
      val agent       = Prefix(ESW, "primary")
      val seqComp     = ComponentId(Prefix(ESW, seqCompName), ComponentType.SequenceComponent)

      when(securityDirectives.sPost(eswUserPolicy)).thenReturn(accessTokenDirective)
      when(sequenceManagerApi.spawnSequenceComponent(agent, seqCompName))
        .thenReturn(Future.successful(SpawnSequenceComponentResponse.Success(seqComp)))

      Post("/post-endpoint", SpawnSequenceComponent(agent, seqCompName).narrow) ~> route ~> check {
        verify(securityDirectives).sPost(eswUserPolicy)
        verify(sequenceManagerApi).spawnSequenceComponent(agent, seqCompName)
        responseAs[SpawnSequenceComponentResponse] should ===(SpawnSequenceComponentResponse.Success(seqComp))
      }
    }

    "return agent status for all running agents | ESW-349" in {
      val response = AgentStatusResponse.Success(List.empty[AgentSeqCompsStatus])
      when(sequenceManagerApi.getAgentStatus).thenReturn(Future.successful(response))

      Post("/post-endpoint", GetAgentStatus.narrow) ~> route ~> check {
        verify(sequenceManagerApi).getAgentStatus
        responseAs[AgentStatusResponse] should ===(response)
      }
    }
  }
}
