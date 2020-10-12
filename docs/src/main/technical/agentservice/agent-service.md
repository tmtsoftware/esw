# Agent Service

## Introduction

The Agent Service is a http server which is used to spawn a component of machine or kill a running component.
To do so, it uses the agent running on that specific machine where component is to be spawned or killed.

## Implementation Details

Agent Service is a http service build using `msocket` framework. It has following layers:

- Server (which interacts with the Http Interface)
- Implementation layer (which includes creating the `AgentClient` for the specific agent)
- Agent (which spawns or kills the given component)

![AgentService](../../images/agentservice/AgentService.svg)

## Module (esw-agent-service)
Implementation of Agent Service is all distributed within following submodules:

### esw-agent-service-api

All the request models and APIs related to `AgentService` resides within this module. 
This also contains the codecs for the models. 
This module depends on `esw-agent-akka-api` module which provides all the response models.

It is a cross-compiled project which has following parts:

- js - code which is related to scala-js.
- jvm - code which is related to jvm
- shared - code which can be used by both scala-js and jvm

### esw-agent-service-impl

This module depends on the `esw-agent-service-api` module. 
It contains the AgentService API's implementation which has the logic of 
creation of the `AgentClient` for given specific agent and calling the specific APIs of the AgentClient. 

### esw-agent-service-app

This module contains all the http handlers, server wiring and, the cli app to start the server. 



