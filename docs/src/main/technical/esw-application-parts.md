# ESW Application Architecture

The applications in ESW are not CSW Assemblies and HCDs, they are registered in the Locaton Service with ComponentType `Service`. The ESW applications have been
constructed in a similar way to make the code easier to understand and test. This section describes the parts of an ESW application
and instroduces the terminology used in the code base. ESW applications generally support both Akka-based communication as well
as HTTP-based communication and are registered in the Location Service with both interfaces.

###The Main Class
The main class is the starting point of the application. Here we specify what arguments the application accepts. 
An ESW main class extends [EswCommandApp]($github.base_url$/esw-commons/src/main/scala/esw/commons/cli/EswCommandApp.scala) which inherits 
from `CommandApp` which is part of [CaseApp](https://github.com/alexarchambault/case-app) 
an external utility package ESW depends on to capture program arguments in a command model class. 
With CaseApp, main class arguments have `commands` and `options`. A class is associated with each supported command and that class 
has fields corresponding to each supported program option. These model classes represent the command and its options.

Examples of main classes are:

* The User Interface Gateway Service application class is: [GatewayMain]($github.base_url$/esw-gateway/esw-gateway-server/src/main/scala/esw/gateway/server/GatewayMain.scala) 
and the `start` command class is: [ServerCommand]($github.base_url$/esw-gateway/esw-gateway-server/src/main/scala/esw/gateway/server/ServerCommand.scala).
* The Agent Service application class is: [AgentServiceApp]($github.base_url$/esw-agent-service/esw-agent-service-app/src/main/scala/esw/agent/service/app/AgentServiceApp.scala)
and the `start` command class is: [AgentServiceAppCommand]($github.base_url$/esw-agent-service/esw-agent-service-app/src/main/scala/esw/agent/service/app/AgentServiceAppCommand.scala).

###The Wiring Class
When the main class executes it uses the command and arguments to create a new instance of a `Wiring` class.
The `Wiring` class is the place where all resources or dependencies the application uses are initialized.

This class takes care of creating and initializing instances of any other dependencies required by the 
application such as an `ActorSystem`, API client instances, setting overrides, initializing communication including starting the appication's HTTP Server, and assigning handlers for HTTP routes. 
`Wiring` will create any clients needed to connect to other services like Location Service client, Event Service client, or AAS, etc.

Examples of application `Wiring`:

* For the User Interface Gateway, the wiring class is: [GatewayWiring]($github.base_url$/esw-gateway/esw-gateway-server/src/main/scala/esw/gateway/server/GatewayWiring.scala).
* For the Agent Service, the wiring class is: [AgentServiceWiring]($github.base_url$/esw-agent-service/esw-agent-service-app/src/main/scala/esw/agent/service/app/AgentServiceWiring.scala).
* For the Sequence Manager, the wiring class is: [SequenceManagerWiring]($github.base_url$/esw-sm/esw-sm-app/src/main/scala/esw/sm/app/SequenceManagerWiring.scala).

@@@ note { title=Hint }
ESW architecture classes are usually named to describe their function in the application architecture (e.g., SequenceManagerWiring or AgentServiceAppMain).
@@@

### The Http Service
In addition to Akka-based communicaton, most of the ESW applications use an HTTP Server to handle HTTP-based requests. Hence we have extracted to a common module [esw-http-core]($github.base_url$/esw-http-core/src), which has code common to all ESW HTTP-based communication for applications. 
The [HttpService]($github.base_url$/esw-http-core/src/HttpService.scala) class is responsible for initializing an HTTP Server and registering its connection information with the Location Service.
Module `esw-http-core` has other common classes like [ActorRuntime]($github.base_url$/esw-http-core/src/HttpService.scala), a wrapper containing actor related class references,
and [Settings]($github.base_url$/esw-http-core/src/Settings.scala), containing logic to extract settings common to all ESW HTTP servers.

@@@ note
The User Interface Gateway is only registered as an HTTP service in Location Service. Because of its role in the TMT architecture as the gateway for HTTP requests from browser-based user interfaces,
and its role in authentication of users, it does not provide a public Akka location in the Location Service.
@@@

### Route Handlers
An HTTP Service uses routes, which map the incoming requests to the code that uses the information in the request to handle the request. A `route handler`is the application-specific code 
that maps the request to the application code. There may be two types of route handlers in an ESW appication: PostHandler and WebsocketHandler.

@@@ note
ESW infrastructure services do not use REST-like path-oriented requests. Rather, all requests are encoded as JSON and POSTed to the service. This is common in services that are
not directly user-facing. This also simplifies the route handling since there are only a few routes. The content of the requests is documented in the contracts.
@@@

####PostHandler
This handler implements routes corresponding to HTTP POST requests. The underlying infrastructure used to handle HTTP requests has been placed in a common 
ESW library called [msocket](https://github.com/tmtsoftware/msocket). 

Examples of PostHandler are:

* For the User Interface Gateway, the POST route handler is: [GatewayPostHandler]($github.base_url$/esw-gateway/esw-gateway-server/src/main/scala/esw/gateway/server/handlers/GatewayPostHandler.scala).
* For the Agent Service, the POST route handler is: [AgentServicePostHandler]($github.base_url$/esw-agent-service/esw-agent-service-app/src/main/scala/esw/agent/service/app/handlers/AgentServicePostHandler.scala).

####WebsocketHandler
Some applications need to keep open connections. For instance, the User Interface Gateway needs to subscribe to events from Event Service or wait for command responses fom Command Service.
In this case, a client posts a subscription request and the result is a WebSocket. 

Since, WebSocket requests work on top of the HTTP protocol, routes in the WebsocketHandler are also handled by `HttpService` along with PostHandler routes.

Examples of WebsocketHandler are:

* For the User Interface Gateway, the WebSocket handler is: [GatewayWebsocketHandler]($github.base_url$/esw-gateway/esw-gateway-server/src/main/scala/esw/gateway/server/handlers/GatewayWebsocketHandler.scala).
* For the Sequencer, the WebSocket handler is: [SequencerWebsocketHandler]($github.base_url$/esw-ocs/esw-ocs-handler/src/main/scala/esw/ocs/handler/SequencerWebsocketHandler.scala).

@@@ note
Applications that do not need streaming data do not have a WebSocketHandler. For instance, the Agent Service does not have a WebSocketHandler.
@@@

###API Classes
The Application Programmer Interface or API classes define the functionality of the service. The API class is used and implemented by the service clients.

For reference, the following figure shows how the API classes and the other classes in this section are related.

![ESW App Parts](../images/techdocs/ESWAppFigures1.png)

Examples of service API classes are:

* For the Sequencer, the API is:  [SequencerApi]($github.base_url$/esw-ocs/esw-ocs-api/shared/src/main/scala/esw/ocs/api/SequencerApi.scala).
* For Sequence Manager, the API is: [SequencerManagerApi]($github.base_url$/esw-sm/esw-sm-api/shared/src/main/scala/esw/sm/api/SequenceManagerApi.scala).

Because an ESW application often provides both an Akka-based client and an HTTP-based client, the API class may be used in 2 places.

###Impl Classes (Akka Clients)
The Impl or implementaton classes are one implementation of the API. In an ESW application based on Akka, the service itself is implemented as Akka-based actors in the `Behavior classes`.

The API is written as a typical API with methods that are called and return results.  Most of the time, the impl classes are converting the API method to an Akka-based message and sending
the message to the Behavior actor. If it gets a response as a message, it converts it to the correct type for the API.

Examples of impl or Akka client classes are:

* For the Sequencer, the impl is:  [SequencerImpl]($github.base_url$/esw-ocs/esw-ocs-api/jvm/src/main/scala/esw/ocs/api/actor/client/SequencerImpl.scala).
* For Sequence Manager, the impl is: [SequencerManagerImpl]($github.base_url$/esw-sm/esw-sm-api/jvm/src/main/scala/esw/sm/api/actor/client/SequenceManagerImpl.scala).

These impl classes can call other services to fulfill the requests or handle it locally using the service's Behaviour classes.

###Behavior Classes
At their core, the ESW applications are implemented as Akka Actors. A `behavior class` is the top level Akka implementation of the service. It is called a behavior because that's what Akka calls the type
returned by a newly constructed typed actor.

Behavior classes receive Akka messages, which are often defined in a `protocol` package such as [this]($github.base_url$/esw-sm/esw-sm-api/shared/src/main/scala/esw/sm/api/protocol/), which includes two message 
files for the Sequence Manager requests and responses. The Impl classes are clients, and don't have logic to manage the application state; state and functionality is handled by the actor behavior classes. 
State-based functionality is often implemented using the Akka actor state machine pattern.

Examples of behavior classes are:

* For the Sequencer, the behavior is:  [SequencerBehavior]($github.base_url$/esw-ocs/esw-ocs-impl/src/main/scala/esw/ocs/impl/core/SequencerBehavior.scala).
* For Sequence Manager, the behavior is: [SequencerManagerBehavior]($github.base_url$/esw-sm/esw-sm-impl/src/main/scala/esw/sm/impl/core/SequenceManagerBehavior.scala).
* For Agent, the behavior is: [AgentActor]($github.base_url$/esw-agent-akka/esw-agent-akka-app/src/main/scala/esw/agent/akka/app/AgentActor.scala).

###Serialization and Codecs
When a request is sent over the network to a service, it needs to be converted from the programming language model and sent over the network in some agreed upon format. Serialization 
is the conversion from the programming language model to the network format, and deserialization the conversion back from the network format to the programming language representation.
In CSW/ESW Akka messages between remote actors are serialized into a format called Concise Binary Object Representation or CBOR (see [CBOR RFC8949 standard](https://cbor.io)). 
Messages using the HTTP transport are serialized and deserialized to Javscript Object Notation or JSON (see [JSON spec](https://www.json.org/json-en.html)). 

For serialization, CSW/ESW applications use the open-source [Borer](https://sirthias.github.io/borer/) library to automatically generate serialization code that converts model classes to/from the JSON and CBOR formats.
One big advantage of this library over others is that it can serialize/deserialize to JSON as well as CBOR.

####Codec Classes
In many cases Borer will _automatically_ generate serialization classes or `codecs`, but messages must be explicitly indicated with a Borer library call.
For example in [OcsCodecs]($github.base_url$/esw-ocs/esw-ocs-api/shared/src/main/scala/esw/ocs/api/codecs/OcsCodecs.scala), the codec for a step in a sequence is created with this line:

```
implicit lazy val stepCodec: Codec[Step] = deriveCodec 
```

Here `deriveCodec` does the job of generating decoder/encoder code for serialization/deserialization. 
These codec classes are parameterized with the model case classes and are marked implicit so that when a class extends this codec class it gets the model class codec automatically.
You can even have hierarchy of model classes and only need to provide a top level class/marker trait to automatically derive child class 
codecs. For example in `SequencerServiceCodecs`: 

```
implicit lazy val sequencerPostRequestValue: Codec[SequencerRequest]  = deriveAllCodecs
```
Here you see `deriveAllCodecs`, which causes the generation of decoder/encoder for the complete class hierarchy starting from the top level `SequencerRequest`.

There are mainly two use cases when you need to serialize/deserialize a request and response.

####Remote Actor Communication
The first use case is when an ESW application actor system needs to interact with other remote actor systems from other services with non-CSW parameter-based messages. 
The CBOR format is uniformly used for serialization/deserialization in this case. 
For example, when a Sequencer receives requests from the User Interface Gateway via Akka actors due to a UI call, the message is serialized as CBOR.

The first step for the Akka/CBOR case is to add Borer codecs for message model classes so that encoder/decoders can be derived as described above. 
An example is: [OcsMsgCodecs]($github.base_url$/esw-ocs/esw-ocs-api/jvm/src/main/scala/esw/ocs/api/actor/OcsMsgCodecs.scala) that extends `OcsCodecs` used above.
This example shows the Borer Codec type parameterized with the message class model classes that are sent to Sequencer in requests and responses. 

In order for the Akka infrastructure to see and use the Borer serializing code, a class is created that inherits from
the abstract Akka serializer infrastructure class: `CborAkkaSerializer` that is part of the CSW code and
`register` each of the service message classes. For the Sequencer messages, 
this class is called [OcsAkkaSerializer]($github.base_url$/esw-ocs/esw-ocs-api/jvm/src/main/scala/esw/ocs/api/actor/OcsAkkaSerializer.scala). The
message classes must also be marked as Serializable, which in this case is done with the [OcsAkkaSerializable]($github.base_url$/esw-ocs/esw-ocs-api/shared/src/main/scala/esw/ocs/api/codecs/OcsAkkaSerializable.scala) trait.
Both the serializer and the serializable class are present in the configuration of application (through the classpath) so that Akka actors can use them for 
serialization/deserialization. 

The final step needed is configuration to make sure Akka is aware of the special serialization by hooking OcsAkkaSerializer
into the Akka infrastructure. For the Sequencer (and any Akka-based app), this is done in the [resource.conf]($github.base_url$/esw-ocs/esw-ocs-api/jvm/src/main/resources/resources.conf) file of the clients of Sequencer.
As shown below, the `ocs-framework-cbor` property is set to the class name of `OcsAkkaSerializer`, and whenever a class is marked with `OcsAkkaSerializable` Akka will use the `ocs-framework-cbor` serializer.

```
akka.actor {
  serializers {
    ocs-framework-cbor = "esw.ocs.api.actor.OcsAkkaSerializer"
  }
  serialization-bindings {
    "esw.ocs.api.codecs.OcsAkkaSerializable" = ocs-framework-cbor
  }
  provider = remote
}

```
This resource file is part of the esw-ocs-api package so any client or service that depends on this api jar file will be configured to serialize and deserialize Akka CBOR-based messages. 

####HTTP-based Communication
The second use case is when the request and response are using the HTTP protocol. In this case, the client uses a `postClient`, which uses the `msocket` library.
The critical part of this is that to use `msocket` the request must be serialized to JSON to be included as the payload to an HTTP POST method.

An example of this is the HTTP client for Sequencer: [SequencerClient]($github.base_url$/esw-ocs/esw-ocs-api/shared/src/main/scala/esw/ocs/api/client/SequencerClient.scala), which is 
created in the [SequencerApiFactory]($github.base_url$/esw-ocs/esw-ocs-api/jvm/src/main/scala/esw/ocs/api/actor/client/SequencerApiFactory.scala) (which also creates the Akka client).
The client code must be constructed with the serialization codecs for the HTTP-based requests and responses.
For example, [SequencerServiceCodecs]($github.base_url$/esw-ocs/esw-ocs-api/shared/src/main/scala/esw/ocs/api/codecs/SequencerServiceCodecs.scala) extends `OcsCodecs` mentioned above.
`SequencerClient` extends this and has access to all the needed codecs. 

When a request goes to a PostHandler in the server, it needs to deserialize the JSON request to a model class, process it, serialize the response to JSON, and return it to the caller.
As discussed previously, the `wiring` classes set the application up. The [SequencerWiring]($github.base_url$/esw-ocs/esw-ocs-app/src/main/scala/esw/ocs/app/wiring/SequencerWiring.scala) extends
[SequencerServiceCodecs]($github.base_url$/esw-ocs/esw-ocs-api/shared/src/main/scala/esw/ocs/api/codecs/SequencerServiceCodecs.scala) and creates the `postHandler`.
Since the WebSocket protocol works on top of the HTTP protocol, WebsocketHandler uses the same mechanism as PostHandler to serialize and deserialize.

The complete flow of classes discussed above is:

Main (using case app) -> Wiring -> HttpService(esw-http-core) -> HttpPostHandler(using Codecs) + WebsocketHandler(using Codecs) -> Behaviour classes(using Codecs).
