# Running Multiple ESW Applications with esw-services

Like `csw-services`, `esw-services` is an application that allows easily starting all the services offered by 
Executive Software in a development environment. When no options are provided, it starts all the services. 
Starting specific services is possible by passing arguments corresponding to specific services when starting.

`esw-services` can help users start: 

* An Agent app, which is responsible for starting Sequence Components, Sequence Manager and Containers on different machines.

* The Agent Service, which helps in forwarding HTTP Agent-related request to distributed Agents. 

* The User Interface Application Gateway, that handles CSW traffic to and from user interfaces.

* Sequence Manager (in Simulation Mode or without Simulation), for provisioning and configuring Sequencers

@@@ note
All the above services are part of a single JVM process except Sequence Manager.  If this scenario is
not acceptable, start the applications using their individual applications.
@@@

## 1. Install `coursier` and the TMT Apps Channel

The `coursier` application must be installed on your machine and the OCS Apps channel must be installed.
The instructions for doing this are provided @ref:[here](getting-apps.md).

## 2. Starting esw-services using coursier

With `coursier` and apps installed, it can be used to start `esw-services`. Type: 

```bash
cs launch esw-services -- start` 
```
If you need to start with a specific version, the following can be used:

```bash
cs launch esw-services:<ESW version | SHA> -- start` 
```

Normally, you don't need to start the app with a specific version.

## 3. Install esw-services using coursier

If you use esw-services extensively, you can create a launcher with:

```bash
cs install esw-services
```
Which creates a launcher for the latest version. Do this whenever a new ESW version is released.
Once this is done, esw-services can be started by just typing `esw-services` and not `cs launch esw-services`.

## Available options while starting esw-services

To start `esw-services` with specific options, append the parameters below to the command for starting `esw-services` mentioned above.

For Agent:

- `--agent | -a`: Start agent with default ESW.primary prefix
- `--agent-prefix`: Start agent with this prefix
- `--agent-service`: Start agent service
- `--host-config-path`: Start containers at time of agent boot up with provided host config path

For Gateway:

- `--gateway | -g`: Start gateway with default command role config
- `--command-role-config`: Start gateway with this command role mapping file

For Sequence Manager (SM):

- `--sequence-manager | -s`: Start SM with default obsMode config
- `--obs-mode-config`: Start SM with this observation mode config file
- `--simulation`: Start SM in simulation mode

## Start esw-services with OCS-ENG-UI
When testing the OCS-ENG-UI, `esw-services` provides convenience options that create a system test configuration that
allows full testing of OCS-ENG-UI with little effort.

One can start the services the Engineering UI needs by starting esw-services with sbt inside the ESW source directory.

`sbt "esw-services/run start-eng-ui-services"`

The above command with the start-eng-ui-services option, starts all ESW services 
such as Agents representing different machines for subsystems like ESW, WFOS, TCS, IRIS, and AOESW. 
It also starts the Sequence Manager needed for the Engineering UI along with Agent Service, and UI Application Gateway. 

To start Engineering UI services with Sequence Manager in Simulation Mode, type:

`sbt "esw-services/run start-eng-ui-services --sm-simulation-mode"`

To start Engineering UI services with a specific sequencer-script version, type:

`sbt "esw-services/run start-eng-ui-services --scripts-version 0.1.0-SNAPSHOT"`
