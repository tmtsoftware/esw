# Applications

@@ toc { .main depth=1 }

@@@ index
* [sequencer-app](sequencer-app.md): Starts SequenceComponent and/or Sequencer.
* [sequence-manager-app](sequence-manager-app.md): Starts Sequence Manager and Sequence Manager HTTP server.
* [agent-service-app](agent-service-app.md): Starts Agent Service server.
* [agent-app](agent-app.md): Starts Agent actor.
* [gateway-app](gateway-app.md): Starts ESW Gateway.
@@@

## Prerequisites

For starting and running ESW apps for development, you need to run CSW services like `location-server`, `event-service` etc.
Please refer to the doc @extref[here](csw:commons/apps) to run CSW services.


## Getting ESW Apps

Developer has to download `esw-apps-<some-version>.zip` from [esw github releases](https://github.com/tmtsoftware/esw/releases) and unzip it.
There are two folders, as follows, in `esw-apps-<some-version>`

* bin
* lib

All the apps provided by ESW reside in bin folder.
