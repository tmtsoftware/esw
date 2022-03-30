/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.http.core.wiring

import akka.actor.CoordinatedShutdown.UnknownReason
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives.{complete, get, path}
import akka.http.scaladsl.server.Route
import com.typesafe.config.Config
import csw.location.api.CswVersionJvm
import csw.location.api.exceptions.OtherLocationIsRegistered
import csw.location.api.models.{ComponentType, HttpRegistration, Metadata, NetworkType}
import csw.location.client.ActorSystemFactory
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory
import csw.network.utils.{Networks, SocketUtils}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.ocs.testkit.EswTestKit

import scala.concurrent.duration.DurationInt

class HttpServiceTest extends EswTestKit {

  private val route: Route =
    path("hello") {
      get {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
      }
    }
  private val hostname                                 = Networks(NetworkType.Outside.envKey).hostname
  implicit override val patienceConfig: PatienceConfig = PatienceConfig(10.seconds, 100.millis)
  var actorRuntime: ActorRuntime                       = _

  override protected def afterEach(): Unit = {
    cleanup(actorRuntime)
    super.afterEach()
  }

  class TestSetup(_servicePort: Int, prefix: Option[Prefix] = None) {
    val actorSystem: ActorSystem[SpawnProtocol.Command] =
      ActorSystemFactory.remote(SpawnProtocol(), "http-core-server-system")
    actorRuntime = new ActorRuntime(actorSystem)
    val config: Config = actorSystem.settings.config
    val settings       = new Settings(Some(_servicePort), prefix, config, ComponentType.Service)
    val loggerFactory  = new LoggerFactory(settings.httpConnection.prefix)
    val logger: Logger = loggerFactory.getLogger
  }

  "HttpService" must {
    "start the http server and register with location service with metadata | ESW-86, CSW-96, ESW-366" in {
      val _servicePort = 4005
      val testSetup    = new TestSetup(_servicePort)
      import testSetup._

      val httpService = new HttpService(logger, locationService, route, settings, actorRuntime)
      val metadata    = Metadata().withCSWVersion(new CswVersionJvm().get).add("key1", "value")

      SocketUtils.isAddressInUse(hostname, _servicePort) shouldBe false

      val (_, registrationResult) = httpService.startAndRegisterServer(metadata).futureValue

      locationService.find(settings.httpConnection).futureValue.get.connection shouldBe settings.httpConnection
      val location = registrationResult.location
      location.uri.getHost should ===(hostname)
      location.connection should ===(settings.httpConnection)
      location.metadata should ===(metadata)
      // should not bind to all but specific hostname IP
      SocketUtils.isAddressInUse(hostname, _servicePort) should ===(true)
      SocketUtils.isAddressInUse("localhost", _servicePort) should ===(false)
    }

    "start the http server and register with location service with empty metadata if not provided while registration | ESW-366" in {
      val _servicePort = 4005
      val testSetup    = new TestSetup(_servicePort)
      import testSetup._

      val httpService = new HttpService(logger, locationService, route, settings, actorRuntime)

      SocketUtils.isAddressInUse(hostname, _servicePort) shouldBe false

      val (_, registrationResult) = httpService.startAndRegisterServer().futureValue

      locationService.find(settings.httpConnection).futureValue.get.connection shouldBe settings.httpConnection
      registrationResult.location.metadata should ===(Metadata().withCSWVersion(new CswVersionJvm().get))

    }

    "not register with location service if server binding fails | ESW-86, CSW-96" in {
      val _servicePort = 4452 // Location Service runs on this port
      val testSetup    = new TestSetup(_servicePort)
      import testSetup._

      val httpService     = new HttpService(logger, locationService, route, settings, actorRuntime)
      val address         = s"[/$hostname:${_servicePort}]"
      val expectedMessage = s"Bind failed because of java.net.BindException: $address Address already in use"

      val bindException = intercept[Exception] { httpService.startAndRegisterServer().futureValue }

      bindException.getCause.getMessage shouldBe expectedMessage
      locationService.find(settings.httpConnection).futureValue shouldBe None
    }

    "not start server if registration with location service fails | ESW-86" in {
      val _existingServicePort = 21212
      val _servicePort         = 4007
      val testSetup            = new TestSetup(_servicePort)
      import testSetup._

      val httpService = new HttpService(logger, locationService, route, settings, actorRuntime)
      locationService
        .register(HttpRegistration(settings.httpConnection, _existingServicePort, "", NetworkType.Outside))
        .futureValue
      locationService.find(settings.httpConnection).futureValue.get.connection shouldBe settings.httpConnection

      SocketUtils.isAddressInUse(hostname, _servicePort) shouldBe false

      val otherLocationIsRegistered = intercept[Exception](httpService.startAndRegisterServer().futureValue)

      otherLocationIsRegistered.getCause shouldBe a[OtherLocationIsRegistered]
      SocketUtils.isAddressInUse(hostname, _servicePort) shouldBe false
    }

    "start the http server on inside network | ESW-511" in {
      val insideHostname = Networks(NetworkType.Inside.envKey).hostname
      val _servicePort   = 4008
      val testSetup      = new TestSetup(_servicePort, Some(getRandomAgentPrefix(ESW)))
      import testSetup._

      val httpService = new HttpService(logger, locationService, route, settings, actorRuntime, NetworkType.Outside)
      val metadata    = Metadata().withCSWVersion(new CswVersionJvm().get).add("key1", "value")

      SocketUtils.isAddressInUse(insideHostname, _servicePort) shouldBe false

      val (_, registrationResult) = httpService.startAndRegisterServer(metadata).futureValue

      locationService.find(settings.httpConnection).futureValue.get.connection shouldBe settings.httpConnection
      val location = registrationResult.location
      location.uri.getHost should ===(insideHostname)
      location.connection should ===(settings.httpConnection)
      location.metadata should ===(metadata)
      // should not bind to all but specific hostname IP
      SocketUtils.isAddressInUse(insideHostname, _servicePort) should ===(true)
      SocketUtils.isAddressInUse("localhost", _servicePort) should ===(false)
    }
  }

  private def cleanup(actorRuntime: => ActorRuntime) = {
    // cleanup which unregisters location
    actorRuntime.shutdown(UnknownReason).futureValue
  }
}
