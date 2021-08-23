# ESW Low Level Design

Goal of this section is to provide walk through of the low level design of the ESW applications.
This section is targeted towards future maintainers of the ESW. It should help them understand different parts of the ESW applications and easily navigate through the codebase.

ESW applications are divided into following three major categories:

1. Actor based, for example, Agent Akka Application.
1. HTTP based, for example, Agent Service, Gateway Application.
1. Embedded Http + Actor based - Such applications exposes two protocols for communication, one is HTTP and other is Akka. for example, ESW OCS (Sequencer) Application.

Most if not all the ESW applications follows the following conventions for organizing the codebase:

1. **Main** - Runnable application responsible for starting the ESW service.
1. **Wiring** - Initializes all the dependencies and wires them together.
1. **HttpService** - Starts and registers the HTTP service with Location Service.
1. **Route Handlers** - Defines two types of the routes for the HTTP service i.e. HTTP routes and Websocket routes.
1. **API** - Interface defining the contract for the Service
1. **Impl** - Server side implementation of the API.
1. **Clients** - Two types of clients implementing API i.e. HTTP and Actor clients.
1. **Actor Behavior** - Defines the behavior of the Service. This includes actual implementation of backend service logic.
1. **Codecs** - Encoders and Decoders required for the remote communication. This includes codecs required for Actors and HTTP service.

## Main Class

Main class is the entry point for all the ESW applications, and responsible for specifying the command line arguments, options and parsing them to domain models.
This capability is provided by extending Main class with `CommandApp` which is coming from a [case-app](https://github.com/alexarchambault/case-app).
**case-app** is a command line argument parsing library for scala
`Command` class has fields corresponding to each supported program argument.
For ESW applications, we use a rich wrapper class `EswCommandApp` build on top of `CommandApp`.
Main class creates instance of `Wiring` class and initializes actors and HTTP server wherever applicable.

Examples of Main class for different application modules are,

1. For Gateway Service application, refer classes `GatewayMain` and `ServerCommand`
1. For Agent Service application, refer `AgentServiceApp`, `AgentServiceAppCommand`

## Wiring Class

Wiring class initializes all the dependencies and resources required for creating Actors and HTTP server.
Some of the common examples of instances and resources created by `Wiring` are:
`ActorSystem`, API implementation instances, Application Settings, `HttpService`, HTTP and Websocket handlers and clients to connect to other services like Location Service, Event Service etc.

Examples of `Wiring` class are, `GatewayWiring`, `AgentServiceWiring`, `SequenceManagerWiring` etc.

## HttpService

As mentioned in the introductory section, many ESW applications for example,
Gateway, ocs-app (Sequencer), Sequence Manager etc. are either HTTP based or Embedded HTTP + Actor based,
and requires capabilities to start and register with Location Service.

We have extracted out these common capabilities in independent module called `esw-http-core`.
This module has class `HttpService` which is responsible for initializing a HTTP Server and registering it with Location Service.

`esw-http-core` module has other common utilities like,

1. `ActorRuntime` - Small wrapper over `ActorSystem` responsible for providing implicit `ActorSystem`, `ExecutionContext`, starting Logging Service and closing resources on shutdown.
1. `Settings` - Reading application specific configuration like service prefix, HttpConnection (Used to register with Location Service) etc.

## HTTP and Websocket Handlers

All the HTTP based applications implements `PostHandler` and `WebsocketHandler` traits provided by [msocket](https://github.com/tmtsoftware/msocket) library.

1. PostHandler - This handler contains routes corresponding to http requests. e.g. `GatewayPostHandler`, `AgentServicePostHandler`.
Since the underlying infrastructure used to handle Http requests is using Msocket library, all requests are of type `POST`.

1. WebsocketHandler - This handler contains routes corresponding to web-socket requests. e.g. `GatewayWebsocketHandler`, `SequencerWebsocketHandler`.
Since, websocket request work on top of http protocol, routes in WebsocketHandler are handled by `HttpService` along with PostHandler routes.

## API

These classes define the contracts for external users of our application. e.g. `SequencerApi`, `SequenceManagerApi` etc.

## Impl

Impl class is present at server side and implements API (service contract). In most of the cases,
these implementations just sends local message to underlying actor which consist of all the business logic or
call other services to fulfil requests. e.g. `SequencerImpl`, `SequenceManagerImpl` etc.

## Actor Behavior

Most of the business logic and functionality resides within the actor. Actors are responsible for managing application state and implementing requirements specific state machines.
e.g. `SequencerBehavior`, `SequenceManagerBehavior`.

## Codecs

Codec stands for encoder and decoder. Codecs are responsible for encoding and decoding akka messages/http requests.

For this purpose, we use [borer](https://sirthias.github.io/borer/) library to derive codecs for our domain models.
Borer provides efficient CBOR and JSON (de)serialization for Scala.

Codecs are required at two places:

1. Remote Actor Communication (Akka actor)
1. HTTP Communication (Akka HTTP)

Lets look at each of this use case in details below.

### Remote Actor Communication (Akka actor)

Actors registers their location with Location Service when they are created. Using this location,
other actors or services running on different JVM's or machine can communicate by message passing.
These messages have to undergo some form of serialization (i.e. the objects have to be converted to and from byte arrays).
This is where codecs come into play. We use [CBOR](https://cbor.io/) format for encoding/decoding actor messages.

For Akka to know which Serializer to use for what, we need to edit our configuration:
in the `akka.actor.serializers`-section, we need to bind names to implementations of the `akka.serialization.Serializer` you wish to use, like this:

```scala
akka.actor {
  serializers {
    ocs-framework-cbor = "esw.ocs.api.actor.OcsAkkaSerializer"
  }
  serialization-bindings {
    "esw.ocs.api.codecs.OcsAkkaSerializable" = ocs-framework-cbor
  }
}
```

Here `OcsAkkaSerializer` class implements `akka.serialization.Serializer` interface and `OcsAkkaSerializable` marker trait is bound to `ocs-framework-cbor` serializer.

With this configuration in place, Akka will know that it should use `OcsAkkaSerializer` for serializing `OcsAkkaSerializable` objects.
Which means all the remote actor messages (incoming/outgoing) needs to extend `OcsAkkaSerializable` marker trait.
These remote messages (only top level in case of sealed ADTs) needs to be registered with `OcsAkkaSerializer`.
This is done by calling `register` method for example, `register[EswSequencerRemoteMessage]`.
`register` method requires implicit codecs in scope for the provided type. In this case, codecs are defined in `OcsMsgCodecs` trait and
`OcsAkkaSerializer` extends from `OcsMsgCodecs`, thats how implicit codecs are brought into scope.

Following example shows how to define codecs for `EswSequencerRemoteMessage` type:

```scala
// OcsMsgCodecs.scala
import io.bullet.borer.derivation.MapBasedCodecs.deriveAllCodec

implicit lazy val eswSequencerMessageCodec: Codec[EswSequencerRemoteMessage] = deriveAllCodecs
```

@@@ note

In case of sealed ADTs, only parent class needs to extend from `OcsAkkaSerializable` marker trait.

@@@

Refer [Akka Serialization](https://doc.akka.io/docs/akka/current/serialization.html) documentation for more details on how to wire up custom CBOR based serializer up with Akka.

Some of the examples for Actor remote message codecs are `OcsCodecs`, `SequencerServiceCodecs` etc.

### HTTP Communication

HTTP based services are implemented using [msocket](https://github.com/tmtsoftware/msocket) library which usage [Akka HTTP](https://doc.akka.io/docs/akka-http/current/) under the hood.

MSocket exposes two factories, `PostRouteFactory[Req]` and `WebsocketRouteFactory[Req]` for generating HTTP and Websocket routes respectively.
Both these factories require implicit decoder in scope, so that Akka HTTP server can decode the incoming HTTP request.
This mechanism is called `Unmarshalling` in Akka HTTPs terminology.
These decoders are brought into scope by `SequencerWiring` extending from `SequencerServiceCodecs`.

Similarly, `PostHandler` and `WebsocketHandler` are responsible for processing incoming HTTP requests and returning HTTP response.
Hence, it requires implicit encoders in scope. This mechanism is called `Marshalling` in Akka HTTPs terminology.
In case of `SequencerPostHandler` and `SequencerWebsocketHandler`, these encoders are brought into scope using following import `import esw.ocs.api.codecs.SequencerServiceCodecs.*`
