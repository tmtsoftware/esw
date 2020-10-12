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

1. For starting and running ESW apps for development, you need to run CSW services like `location-server`, `event-service`, `aas-service` etc.
Please refer to the doc @extref[here](csw:commons/apps) to run CSW services.

2. Install coursier

Please refer to [coursier installation document](https://get-coursier.io/docs/cli-installation)


## Getting ESW Apps

`cs install` command will be used to install executables/launchers of each of ESW apps.

For example following command shows installation for agent app,
```bash
cs install agent-app:<version | SHA>
```

Each app section explains installation and running procedure in detail.
