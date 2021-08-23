# ESW Design Document

This section describes design of an esw component.

There are two type of applications :
Remote Actor based Access like agent akka app
Embeded Http Sever based - These apps uses combination of Actors and Http Layer like Gateway

For each type of access, there exists clients that provides Codecs which helps in serialization/deserialization and App/Server side uses these clients. Like Agent Akka Client project provides a client to connect to Agent Akka App hence it depends on Agent Akka Client for Codecs.

Packages Structure followed in various esw apps

AGENT AKKA
client, app

AGENT HTTP
api, app, impl

GATEWAY
api, server, impl

SEQUENCER
api, app, impl, handler, dsl, dsl-kt

SEQUENCE-MANAGER
api, app, impl, handler

An esw component is composed of various classes layered on top of each other. All these layers are described in sections below.

Main class

 This class is the starting point of any application, Here we specify what arguments the application accepts. Main class extends `CommandApp` which is a [case app](https://github.com/alexarchambault/case-app), it is a utility to capture program arguments in command model class. `Command` class has fields corresponding to each supported program argument. For esw applications, we use a rich wrapper class [EswCommandApp]($github.base_url$/esw-commons/src/main/scala/esw/commons/cli/EswCommandApp.scala) build on top of `CommandApp`.
 When we run Main class it creates a new instance `Wiring` class and triggers Http server startup. How to start Http server and what other things to initialize is decided by `Wiring` class.
e.g.
For Gateway service application refer classes [GatewayMain]($github.base_url$/esw-gateway/esw-gateway-server/src/main/scala/esw/gateway/server/GatewayMain.scala) and [ServerCommand]($github.base_url$/esw-gateway/esw-gateway-server/src/main/scala/esw/gateway/server/ServerCommand.scala)
For Agent service application refer [AgentServiceApp]($github.base_url$/esw-agent-service/esw-agent-service-app/src/main/scala/esw/agent/service/app/AgentServiceApp.scala), [AgentServiceAppCommand]($github.base_url$/esw-agent-service/esw-agent-service-app/src/main/scala/esw/agent/service/app/AgentServiceAppCommand.scala)

Wiring class

  This class takes care of initializating instance of various other classed required by this application like `ActorSystem`, api instances, setting overrides, [HttpService]($github.base_url$/esw-http-core/src/main/scala/esw/http/core/wiring/HttpService.scala) , Handlers for routes and clients to connect to other services like location service client, event service client, security etc.
e.g [GatewayWiring]($github.base_url$/esw-gateway/esw-gateway-server/src/main/scala/esw/gateway/server/GatewayWiring.scala), [AgentServiceWiring]($github.base_url$/esw-agent-service/esw-agent-service-app/src/main/scala/esw/agent/service/app/AgentServiceWiring.scala), [SequenceManagerWiring]($github.base_url$/esw-sm/esw-sm-app/src/main/scala/esw/sm/app/SequenceManagerWiring.scala)

HttpService

 Most of esw applications needs a HTTP server to handle http requests, Hence we have extracted to a common module [esw-http-core]($github.base_url$/esw-http-core), which has things common to all esw http applications. [HttpService]($github.base_url$/esw-http-core/src/main/scala/esw/http/core/wiring/HttpService.scala) class is responsible for initializing a HTTP Server and registering its location to location service.
 Module [esw-http-core]($github.base_url$/esw-http-core) has other common classes like [ActorRuntime]($github.base_url$/esw-http-core/src/main/scala/esw/http/core/wiring/ActorRuntime.scala) wrapper containing actor related class refences, [Settings]($github.base_url$/esw-http-core/src/main/scala/esw/http/core/wiring/Settings.scala) containing logic to extract common settings.

Route handlers

  There are two type of route handlers.
  PostHandler - This handler contains routes corresponding to http requests. e.g. [GatewayPostHandler]($github.base_url$/esw-gateway/esw-gateway-server/src/main/scala/esw/gateway/server/handlers/GatewayPostHandler.scala), [AgentServicePostHandler]($github.base_url$/esw-agent-service/esw-agent-service-app/src/main/scala/esw/agent/service/app/handlers/AgentServicePostHandler.scala). Since the underlying infrastructure used to handle Htpp requests is using [Msocket](https://github.com/tmtsoftware/msocket) library, all requests are of type `POST`.
  WebsocketHandler - This handler contains routes corresponding to web-socket requests. e.g. [GatewayWebsocketHandler]($github.base_url$/esw-gateway/esw-gateway-server/src/main/scala/esw/gateway/server/handlers/GatewayWebsocketHandler.scala), [SequencerWebsocketHandler]($github.base_url$/esw-ocs/esw-ocs-handler/src/main/scala/esw/ocs/handler/SequencerWebsocketHandler.scala).
Since, websocket request work on top of http protocol, routes in WebsocketHandler are handled by [HttpService]($github.base_url$/esw-http-core/src/main/scala/esw/http/core/wiring/HttpService.scala) along with PostHandler reoutes.

Api, Impl and Behavior classes

  Api - These classes define the contracts for external users of our application. e.g. [SequencerApi]($github.base_url$/esw-ocs/esw-ocs-api/shared/src/main/scala/esw/ocs/api/SequencerApi.scala), [SequenceManagerApi]($github.base_url$/esw-sm/esw-sm-api/shared/src/main/scala/esw/sm/api/SequenceManagerApi.scala), [AlarmApi]($github.base_url$/esw-gateway/esw-gateway-api/src/main/scala/esw/gateway/api/AlarmApi.scala).
  Impl - These classes has implementation to Api classes. e.g. [SequencerImpl]($github.base_url$/esw-ocs/esw-ocs-api/jvm/src/main/scala/esw/ocs/api/actor/client/SequencerImpl.scala), [SequenceManagerImpl]($github.base_url$/esw-sm/esw-sm-api/jvm/src/main/scala/esw/sm/api/actor/client/SequenceManagerImpl.scala), [AlarmImpl]($github.base_url$/esw-gateway/esw-gateway-impl/src/main/scala/esw/gateway/impl/AlarmImpl.scala). These classes can call other services to fullfil the requests or handle it using corresponding Behaviour classes.
  Behaviour - Impl classes don't have logic to manage application state of the system, it is handled by Behavious classes. They use Akka Actors state machine pattern to manage state. e.g. [SequencerBehavior]($github.base_url$/esw-ocs/esw-ocs-impl/src/main/scala/esw/ocs/impl/core/SequencerBehavior.scala), [SequenceManagerBehavior]($github.base_url$/esw-sm/esw-sm-impl/src/main/scala/esw/sm/impl/core/SequenceManagerBehavior.scala).

Codecs

 When you send request over the wire, it needs to be converted and send in some standard format like JSON. For this purpose we use Codec classes which use [Borer](https://github.com/sirthias/borer) library to automatically derive JSON and CBOR format. e.g. in [OcsCodecs]($github.base_url$/esw-ocs/esw-ocs-api/shared/src/main/scala/esw/ocs/api/codecs/OcsCodecs.scala) `implicit lazy val stepCodec: Codec[Step] = deriveCodec`, here deriveCode do the job of generating decoder/encoder for serialization/deserialization. These Codec classes contain reference of model case classes and these are marked implicit, so that when a class extends this codec class it gets the model class codec automatically. e.g [OcsCodecs]($github.base_url$/esw-ocs/esw-ocs-api/shared/src/main/scala/esw/ocs/api/codecs/OcsCodecs.scala), [SequencerServiceCodecs]($github.base_url$/esw-ocs/esw-ocs-api/shared/src/main/scala/esw/ocs/api/codecs/SequencerServiceCodecs.scala). You can even have heirarchy of model classes and you just need to provide top level class/marker trait and it will automatically derive child class codecs. e.g. in [SequencerServiceCodecs]($github.base_url$/esw-ocs/esw-ocs-api/shared/src/main/scala/esw/ocs/api/codecs/SequencerServiceCodecs.scala) `implicit lazy val sequencerPostRequestValue: Codec[SequencerRequest]  = deriveAllCodecs` , here you see `deriveAllCodecs` which means generate decoder/encoder of complete class heirarchy starting from top level [SequencerRequest]($github.base_url$/esw-ocs/esw-ocs-api/shared/src/main/scala/esw/ocs/api/protocol/SequencerRequest.scala).
 There are mainly two use cases when you need to serialize/deserialize of request and response.

* Remote Actor Communication - First use case is when your actor system interact with other remote actor systems from other services. We use cbor format for serialization/deserialization in our application for it. e.g. When Sequencer receive requests from Gateway via Actors. For such cases first we need to add model classes to Codecs so that encoder/decoder can be derived. e.g. [AgentActorCodecs]($github.base_url$/esw-agent-akka/esw-agent-akka-client/src/main/scala/esw/agent/akka/client/codecs/AgentActorCodecs.scala). Then we need to registed models classes to serializer class e.g. [AgentAkkaSerializer]($github.base_url$/esw-agent-akka/esw-agent-akka-client/src/main/scala/esw/agent/akka/client/AgentAkkaSerializer.scala) and also we need to mark our model classes as Serializable e.g. `sealed trait AgentResponse extends AgentAkkaSerializable`. Both Serializer and Serializable class are present in configuration of application so that Akka Actors can use them for serialization/deserialization. e.g. in `esw-agent-akka-client` module [reference.conf]($github.base_url$/esw-agent-akka/esw-agent-akka-client/src/main/resources/reference.conf) has [AgentAkkaSerializer]($github.base_url$/esw-agent-akka/esw-agent-akka-client/src/main/scala/esw/agent/akka/client/AgentAkkaSerializer.scala) as serializers and [AgentAkkaSerializable]($github.base_url$/esw-agent-service/esw-agent-service-api/shared/src/main/scala/esw/agent/service/api/AgentAkkaSerializable.scala) as serialization-bindings.
  
* Http Communication - Second use case is when consume your request and send response back over Http protocol. For such use cases, when a request goes to PostHandler, it need a mechanism to deserialize request, process it and serialize the response and sent it back. For this reason PostHandler uses HttpCodec classes which takes care of serialization and deserialization. Since, Web socket protocol work on top of Http protocol hence WebsocketHandler uses same mechanism as PostHandler. e.g. [SequencerWiring]($github.base_url$/esw-ocs/esw-ocs-app/src/main/scala/esw/ocs/app/wiring/SequencerWiring.scala) extends [SequencerServiceCodecs]($github.base_url$/esw-ocs/esw-ocs-api/shared/src/main/scala/esw/ocs/api/codecs/SequencerServiceCodecs.scala) and further [SequencerWiring]($github.base_url$/esw-ocs/esw-ocs-app/src/main/scala/esw/ocs/app/wiring/SequencerWiring.scala) pass Codecs to [SequencerPostHandler]($github.base_url$/esw-ocs/esw-ocs-handler/src/main/scala/esw/ocs/handler/SequencerPostHandler.scala) via `PostRouteFactory` and to [SequencerWebsocketHandler]($github.base_url$/esw-ocs/esw-ocs-handler/src/main/scala/esw/ocs/handler/SequencerWebsocketHandler.scala) via `WebsocketRouteFactory`.

Complete flow of above discussed classes is :

 Main (using case app) -> Wiring -> HttpService(esw-http-core) -> HttpPostHandler(using Codecs) + WebsocketHandler(using Codecs) -> Behaviour classes(using Codecs).
