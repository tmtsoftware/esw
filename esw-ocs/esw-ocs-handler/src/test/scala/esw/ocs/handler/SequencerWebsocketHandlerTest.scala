/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.handler

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.http.scaladsl.model.ws.{BinaryMessage, TextMessage}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}
import akka.stream.scaladsl.Source
import akka.util.Timeout
import csw.params.commands.CommandResponse.{Completed, SubmitResponse}
import csw.params.core.models.Id
import esw.ocs.api.SequencerApi
import esw.ocs.api.codecs.SequencerServiceCodecs
import esw.ocs.api.models.SequencerState.Idle
import esw.ocs.api.models.StepList
import esw.ocs.api.protocol.SequencerStreamRequest.{QueryFinal, SubscribeSequencerState}
import esw.ocs.api.protocol.{SequencerStateResponse, SequencerStreamRequest}
import esw.testcommons.BaseTestSuite
import io.bullet.borer.Decoder
import msocket.api.ContentEncoding.JsonText
import msocket.api.ContentType
import msocket.http.CborByteString
import msocket.http.post.ClientHttpCodecs
import msocket.http.ws.WebsocketExtensions.WebsocketEncoding
import msocket.http.ws.WebsocketRouteFactory
import msocket.jvm.SourceExtension.RichSource
import msocket.jvm.metrics.LabelExtractor
import org.mockito.Mockito.{reset, when}

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong
class SequencerWebsocketHandlerTest
    extends BaseTestSuite
    with ScalatestRouteTest
    with SequencerServiceCodecs
    with ClientHttpCodecs {

  override def clientContentType: ContentType = ContentType.Json

  private val sequencer: SequencerApi = mock[SequencerApi]

  private lazy val websocketHandlerFactory = new SequencerWebsocketHandler(sequencer)

  private implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test-system")

  import LabelExtractor.Implicits.default
  lazy val route: Route =
    new WebsocketRouteFactory[SequencerStreamRequest]("websocket-endpoint", websocketHandlerFactory).make()

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(sequencer)
  }

  "SequencerWebsocketHandler" must {
    "return final submit response of sequence for QueryFinal request | ESW-101" in {
      val wsClient                  = WSProbe()
      val id                        = Id("some")
      implicit val timeout: Timeout = Timeout(10.seconds)
      val completedResponse         = Completed(id)
      when(sequencer.queryFinal(id)).thenReturn(Future.successful(completedResponse))

      WS("/websocket-endpoint", wsClient.flow) ~> route ~> check {
        wsClient.sendMessage(ContentType.Json.strictMessage(QueryFinal(id, timeout): SequencerStreamRequest))
        isWebSocketUpgrade shouldBe true

        val response = decodeMessage[SubmitResponse](wsClient)
        response shouldEqual completedResponse

      }
    }

    "return a sequencer stream for SubscribeSequencerState | ESW-213" in {
      val wsClient               = WSProbe()
      val sequencerStateResponse = SequencerStateResponse(StepList(List.empty), Idle)
      val source                 = Source.repeat(sequencerStateResponse).withSubscription()
      when(sequencer.subscribeSequencerState()).thenReturn(source)
      WS("/websocket-endpoint", wsClient.flow) ~> route ~> check {
        wsClient.sendMessage(ContentType.Json.strictMessage(SubscribeSequencerState: SequencerStreamRequest))
        isWebSocketUpgrade shouldBe true

        val response = decodeMessage[SequencerStateResponse](wsClient)
        response shouldEqual sequencerStateResponse
        source.preMaterialize()._1.cancel()

      }
    }
  }

  private def decodeMessage[T](wsClient: WSProbe)(implicit decoder: Decoder[T]): T = {
    wsClient.expectMessage() match {
      case TextMessage.Strict(text)   => JsonText.decode[T](text)
      case BinaryMessage.Strict(data) => CborByteString.decode[T](data)
      case _                          => throw new RuntimeException("The expected message is not Strict")
    }
  }
}
