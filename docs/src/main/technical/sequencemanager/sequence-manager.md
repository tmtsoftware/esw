# Sequence Manager

## Introduction

Sequence Manager is ESW component which takes care of provisioning sequence components needed for observation, configuration of
sequencer scripts as per observing mode. It has capabilities to start, re-start, shutdown sequencer/s, shutdown sequence components
as well as to know status of TMT components like which sequencer script is loaded on which sequence component, which sequence component/s
are running on which agent. Sequence Manager is implemented using Akka actor. Sequence Manager provides two interfaces.
1. Akka interface
2. HTTP interface

## Sequence Manager Modules

Sequence Manager implementation is distributed into following sub modules:

### esw-sm-api
This sequence manager API module is responsible for providing:

1. shared - API which is cross compiled to JVM as well JS
2. shared - HTTP client which can be used by JVM as well as scala-js applications
3. jvm - Akka client for JVM applications, Akka actor messages, akka serializer

### esw-sm-handler
This sequence manager handler module is responsible for providing HTTP routes for sequence manager HTTP server.

### esw-sm-impl
This module contains core logic for Sequence Manager Actor.

### esw-sm-app
This module contains cli which starts sequence manager component as well as HTTP server of Sequence Manager

