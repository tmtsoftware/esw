#Agent

##Introduction

It is an Actor which is used to spawn or kill a component on a machine where it is running itself.

Currently, it is able to spawn or kill following component

- Sequence Manager
- Sequence Component

##Implementation

Agent is an akka actor which spawns or kill a jvm process depending on the message. 
If actor receives a spawn message(e.g., `SpawnSequenceManager` etc), 
it will first check if the given component is already register if yes, it replies with a failure response 
else it tries to spawn the component.

On any spawn message Agent follows following steps:

- Checks if the component is already registered.

    - If yes it returns the `Failure` response
    - If not then it goes to next step
         
- It spawns the component by initializing a jvm process.

     - If process is successfully spawned it returns the `Spawned` response
     - If process is failed to be spawned it returns the `Failure` response 

##Module (esw-agent-akka)
Implementation of Agent is all distributed within following submodules:

###esw-agent-akka-app

In this module, agent actor's implementation and agent app is present. 
This module depends on [esw-agent-akka-client](#esw-agent-akka-client) for the agent actor's messages and codecs.

###esw-agent-akka-client

In this module, [agent client](#agentclient) and the agent actor messages are present.   


##AgentClient

Agent client is an actor proxy to the agent actor. 
It provides the api which internally send the message to the agent actor 
and returns the responses replied by the agent actor. 
