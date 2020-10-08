# Sequence Manager App

A command line application that facilitates starting Sequence Manager and HTTP server of Sequence Manager.

## Prerequisite

- Location server should be running.
- CSW AAS should be running.

## How to start Sequence Manager App

#### Running sequence manager using Coursier

* Add TMT Apps channel to your local Coursier installation using below command

```bash
cs install --add-channel https://raw.githubusercontent.com/tmtsoftware/osw-apps/master/apps.prod.json
```

* After adding TMT apps channel you can simply launch sequence-manager by executing start command

Start command supports following arguments:

- `-o` : Config file path which has mapping of sequencers and resources needed for different observing modes
- `-l` : optional aregument (true if config is to be read locally or false if from remote server) default value is false
- `-a` : optional argument: agentPrefix on which sequence manager will be spawned, ex: ESW.agent1, IRIS.agent2 etc.
          This argument is used when Sequence Manager is spawned using Agent. For starting standalone sequence manager for testing or on local
          this argument is not needed.


This command starts Sequence Manager as well as HTTP server of Sequence Manager.

### Examples:

```bash
cs launch sequence-manager:<version | SHA> -- start -o obsmode.conf -l
```

```bash
cs launch sequence-manager:<version | SHA> -- start -o obsmode.conf
```

Note: If you don't provide the version or SHA in above command, `sequence-manager` will start with the latest tagged binary of `esw-sm-app`
