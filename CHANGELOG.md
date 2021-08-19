# Change Log

Executive Software (ESW) is a reimplementation/refactoring of the prototype ESW code [here](https://github.com/tmtsoftware/esw-prototype)
developed during the ESW design phase with changes to make the code and public APIs
more robust and resilient and to improve its usability and performance for use at the
TMT Observatory.

Here is the repository for ESW: [ESW](https://github.com/tmtsoftware/esw).

All notable changes to this project will be documented in this file.

## [Upcoming version]

- Added Shutdown, Restart, GoOnline, GoOffline, GetContainerLifecycleState and GetComponentLifecycleState Admin API routes in Gateway.
- Added GetResources, GetObsModesDetails API in the Sequence Manager
- Removed GetRunningObsMode API from Sequence Manager
- Added spawnContainers API to agent app and agent service
- Allow configuring port for HTTP instances of AgentServiceApp & SequenceManagerApp from command line.
- Updated app name's with `esw` prefix according to `apps.prod.json` in `osw-apps` repo.
- Removed `Struct` script dsl helpers. The idea of removal was sent out with the last release.

## [ESW v0.2.1] - 2021-01-29

This is a patch release over v0.2.0 of the TMT Executive Software for project stakeholders.
See [here](https://tmtsoftware.github.io/esw/0.2.1/) for a detailed documentation of this version of the ESW software.

### Changes

Patch update to resolve latest release on Jitpack

### Documentation
- Reference paradox documentation: https://tmtsoftware.github.io/esw/0.2.1/
- Scaladoc: https://tmtsoftware.github.io/esw/0.2.1/api/scala/index.html

## [ESW v0.2.0] - 2021-01-29

This is the second minor release of the TMT Executive Software for project stakeholders.
See [here](https://tmtsoftware.github.io/esw/0.2.0/) for a detailed documentation of this version of the ESW software.

### Changes
Main components are delivered as part of ESW <sup>[1](#0-2-0-1)</sup>:
- Sequence Manager
- Agent
- Agent Service
- CSW shell merged into ESW

Changed Agent Service Kill API to take `ComponentId` instead of `Connection` of the Component.<sup>[2](#0-2-0-2)</sup>

### Version Upgrades
- Scala version upgrade to 2.13.3
- SBT version upgrade to 1.4.2
- Borer version upgrade to 1.6.2
- Akka version upgrade 2.6.10
- Akka-http version upgrade 10.2.1

### Documentation
- Reference paradox documentation: https://tmtsoftware.github.io/esw/0.2.0/
- Scaladoc: https://tmtsoftware.github.io/esw/0.2.0/api/scala/index.html

### Supporting Releases

<a name="0-2-0-1"></a>1: [ESW v0.2.0-M1](https://github.com/tmtsoftware/esw/releases/tag/v0.2.0-M1) - 2020-09-24<br>
<a name="0-2-0-2"></a>2: [ESW v0.2.0-RC1](https://github.com/tmtsoftware/esw/releases/tag/v0.2.0-RC1) - 2020-11-10<br>

## [ESW v0.1.0] - 2020-03-19

This is the first minor release of the TMT Executive Software for project stakeholders.
See [here](https://tmtsoftware.github.io/esw/0.1.0/) for a detailed documentation of this version of the ESW software.

### Changes
Three main components are delivered as part of ESW:
* Sequencer
* Sequence Component
* ESW Gateway

### Documentation
- Reference paradox documentation: https://tmtsoftware.github.io/esw/0.1.0/
- Scaladoc: https://tmtsoftware.github.io/esw/0.1.0/api/scala/index.html

### Supporting Releases

<a name="0-1-0-1"></a>1: [ESW v0.1.0-RC1](https://github.com/tmtsoftware/esw/releases/tag/v0.1.0-RC1) - 2020-02-06<br>
<a name="0-1-0-2"></a>2: [ESW v0.1.0-RC2](https://github.com/tmtsoftware/esw/releases/tag/v0.1.0-RC2) - 2020-02-26<br>
<a name="0-1-0-3"></a>3: [ESW v0.1.0-RC3](https://github.com/tmtsoftware/esw/releases/tag/v0.1.0-RC3) - 2020-03-03<br>
