package esw.sm.handler

import akka.http.scaladsl.server.directives.BasicDirectives
import akka.http.scaladsl.testkit.ScalatestRouteTest
import csw.aas.http.SecurityDirectives
import csw.location.api.models.{ComponentId, ComponentType}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.commons.auth.AuthPolicies
import esw.ocs.api.models.ObsMode
import esw.sm.api.SequenceManagerApi
import esw.sm.api.codecs.SequenceManagerServiceCodecs
import esw.sm.api.models.*
import esw.sm.api.models.ObsModeStatus.Configurable
import esw.sm.api.protocol.*
import esw.sm.api.protocol.SequenceManagerRequest.*
import esw.testcommons.BaseTestSuite
import msocket.api.ContentType
import msocket.http.post.{ClientHttpCodecs, PostRouteFactory}
import msocket.jvm.metrics.LabelExtractor
import msocket.security.models.AccessToken

import scala.concurrent.Future

class SequenceManagerRequestHandlerTest
    extends BaseTestSuite
    with ScalatestRouteTest
    with SequenceManagerServiceCodecs
    with ClientHttpCodecs {
  private val sequenceManagerApi = mock[SequenceManagerApi]
  private val securityDirectives = mock[SecurityDirectives]
  private val postHandler        = new SequenceManagerRequestHandler(sequenceManagerApi, securityDirectives)

  import LabelExtractor.Implicits.default
  private val route = new PostRouteFactory[SequenceManagerRequest]("post-endpoint", postHandler).make()

  private val string10    = randomString(10)
  private val obsMode     = ObsMode(string10)
  private val componentId = ComponentId(Prefix(ESW, obsMode.name), ComponentType.Sequencer)

  private val eswUserPolicy        = AuthPolicies.eswUserRolePolicy
  private val accessToken          = mock[AccessToken]
  private val accessTokenDirective = BasicDirectives.extract(_ => accessToken)

  override def clientContentType: ContentType = ContentType.Json

  override protected def afterEach(): Unit = {
    super.afterEach()
    reset(securityDirectives, sequenceManagerApi)
  }

  implicit class Narrower(x: SequenceManagerRequest) {
    def narrow: SequenceManagerRequest = x
  }

  "SequenceManagerPostHandler" must {
    "return configure success for configure request | ESW-171, ESW-332" in {
      when(securityDirectives.sPost(eswUserPolicy)).thenReturn(accessTokenDirective)
      when(sequenceManagerApi.configure(obsMode)).thenReturn(Future.successful(ConfigureResponse.Success(componentId)))

      Post("/post-endpoint", Configure(obsMode).narrow) ~> route ~> check {
        verify(securityDirectives).sPost(eswUserPolicy)
        verify(sequenceManagerApi).configure(obsMode)
        responseAs[ConfigureResponse] should ===(ConfigureResponse.Success(componentId))
      }
    }

    "return provision success for provision request | ESW-347, ESW-332" in {
      val provisionConfig = ProvisionConfig(Prefix(ESW, "primary") -> 1)
      when(securityDirectives.sPost(eswUserPolicy)).thenReturn(accessTokenDirective)
      when(sequenceManagerApi.provision(provisionConfig)).thenReturn(Future.successful(ProvisionResponse.Success))

      Post("/post-endpoint", Provision(provisionConfig).narrow) ~> route ~> check {
        verify(securityDirectives).sPost(eswUserPolicy)
        verify(sequenceManagerApi).provision(provisionConfig)
        responseAs[ProvisionResponse] should ===(ProvisionResponse.Success)
      }
    }

    "return observation modes with status for getObsModesDetails request | ESW-466" in {
      val expectedObsModesDetailsResponse =
        ObsModesDetailsResponse.Success(Set(ObsModeDetails(obsMode, Configurable, Resources(), Sequencers())))
      when(sequenceManagerApi.getObsModesDetails)
        .thenReturn(Future.successful(expectedObsModesDetailsResponse))

      Post("/post-endpoint", GetObsModesDetails.narrow) ~> route ~> check {
        verify(sequenceManagerApi).getObsModesDetails
        responseAs[ObsModesDetailsResponse] should ===(expectedObsModesDetailsResponse)
      }
    }
    "return start sequencer success for startSequencer request | ESW-171, ESW-332" in {
      when(securityDirectives.sPost(eswUserPolicy)).thenReturn(accessTokenDirective)
      when(sequenceManagerApi.startSequencer(componentId.prefix))
        .thenReturn(Future.successful(StartSequencerResponse.Started(componentId)))

      Post("/post-endpoint", StartSequencer(componentId.prefix).narrow) ~> route ~> check {
        verify(sequenceManagerApi).startSequencer(componentId.prefix)
        verify(securityDirectives).sPost(eswUserPolicy)
        responseAs[StartSequencerResponse] should ===(StartSequencerResponse.Started(componentId))
      }
    }

    "return shutdown sequencer success for shutdownSequencer request | ESW-171, ESW-332" in {
      when(securityDirectives.sPost(eswUserPolicy)).thenReturn(accessTokenDirective)
      when(sequenceManagerApi.shutdownSequencer(ESW, obsMode)).thenReturn(Future.successful(ShutdownSequencersResponse.Success))

      Post("/post-endpoint", ShutdownSequencer(ESW, obsMode).narrow) ~> route ~> check {
        verify(securityDirectives).sPost(eswUserPolicy)
        verify(sequenceManagerApi).shutdownSequencer(ESW, obsMode)
        responseAs[ShutdownSequencersResponse] should ===(ShutdownSequencersResponse.Success)
      }
    }

    "return success for shutdownSubsystemSequencers request | ESW-171, ESW-332" in {
      when(securityDirectives.sPost(eswUserPolicy)).thenReturn(accessTokenDirective)
      when(sequenceManagerApi.shutdownSubsystemSequencers(ESW)).thenReturn(Future.successful(ShutdownSequencersResponse.Success))

      Post("/post-endpoint", ShutdownSubsystemSequencers(ESW).narrow) ~> route ~> check {
        verify(securityDirectives).sPost(eswUserPolicy)
        verify(sequenceManagerApi).shutdownSubsystemSequencers(ESW)
        responseAs[ShutdownSequencersResponse] should ===(ShutdownSequencersResponse.Success)
      }
    }

    "return success for shutdownObsModeSequencers request | ESW-171, ESW-332" in {
      when(securityDirectives.sPost(eswUserPolicy)).thenReturn(accessTokenDirective)
      when(sequenceManagerApi.shutdownObsModeSequencers(obsMode))
        .thenReturn(Future.successful(ShutdownSequencersResponse.Success))

      Post("/post-endpoint", ShutdownObsModeSequencers(obsMode).narrow) ~> route ~> check {
        verify(securityDirectives).sPost(eswUserPolicy)
        verify(sequenceManagerApi).shutdownObsModeSequencers(obsMode)
        responseAs[ShutdownSequencersResponse] should ===(ShutdownSequencersResponse.Success)
      }
    }

    "return shutdown all sequencer success for shutdownAllSequencer request | ESW-171, ESW-332" in {
      when(securityDirectives.sPost(eswUserPolicy)).thenReturn(accessTokenDirective)
      when(sequenceManagerApi.shutdownAllSequencers()).thenReturn(Future.successful(ShutdownSequencersResponse.Success))

      Post("/post-endpoint", ShutdownAllSequencers.narrow) ~> route ~> check {
        verify(securityDirectives).sPost(eswUserPolicy)
        verify(sequenceManagerApi).shutdownAllSequencers()
        responseAs[ShutdownSequencersResponse] should ===(ShutdownSequencersResponse.Success)
      }
    }

    "return restart sequencer success for restartSequencer request | ESW-171, ESW-332" in {
      when(securityDirectives.sPost(eswUserPolicy)).thenReturn(accessTokenDirective)
      when(sequenceManagerApi.restartSequencer(ESW, obsMode))
        .thenReturn(Future.successful(RestartSequencerResponse.Success(componentId)))

      Post("/post-endpoint", RestartSequencer(ESW, obsMode).narrow) ~> route ~> check {
        verify(securityDirectives).sPost(eswUserPolicy)
        verify(sequenceManagerApi).restartSequencer(ESW, obsMode)
        responseAs[RestartSequencerResponse] should ===(RestartSequencerResponse.Success(componentId))
      }
    }

    "return shutdown sequence component success for shutdownSequenceComponent request | ESW-338, ESW-332" in {
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

    "return shutdown sequence component success for shutdownAllSequenceComponent request | ESW-346, ESW-332" in {
      when(securityDirectives.sPost(eswUserPolicy)).thenReturn(accessTokenDirective)
      when(sequenceManagerApi.shutdownAllSequenceComponents())
        .thenReturn(Future.successful(ShutdownSequenceComponentResponse.Success))

      Post("/post-endpoint", ShutdownAllSequenceComponents.narrow) ~> route ~> check {
        verify(securityDirectives).sPost(eswUserPolicy)
        verify(sequenceManagerApi).shutdownAllSequenceComponents()
        responseAs[ShutdownSequenceComponentResponse] should ===(ShutdownSequenceComponentResponse.Success)
      }
    }

    "return resource status for resources | ESW-467" in {
      val response = ResourcesStatusResponse.Success(List.empty[ResourceStatusResponse])

      when(sequenceManagerApi.getResources).thenReturn(Future.successful(response))

      Post("/post-endpoint", GetResources.narrow) ~> route ~> check {
        verify(sequenceManagerApi).getResources
        responseAs[ResourcesStatusResponse] should ===(response)
      }
    }
  }
}
