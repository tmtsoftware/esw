# esw-services

`esw-services` acts like a single application to startup all ESW services on development environment. The main objective
of `esw-services` is to have a user-friendly way of simulating the prod like ESW environment which would provide users with
a platform to test various ESW services and interactions of other apps with these services. You can use `esw-services` to
start following apps/services:

1. Agent app
   - This starts lightweight deamon and expected to be up and running on all the production machines
   - Agent is responsible for starting Sequence Components, Sequence Manager and Containers on provided machines
   - Browsers/UI applications can not directly communicate with agents, they have to go through auth protected Agent Service
   - `esw-services` starts bunch of agents on the same machine simulating multiple machine kind of environment
1. Agent service
   - HTTP service accepts agent related request from the outside network (ex. from browser)
   - Forwards these requests to appropriate agent running on appropriate machine
   - Single Agent Service should be running in production as well as development environment
   - This kind of act as proxy to all the agents
1. Gateway
1. Sequence Manager (in or out of simulation mode)

Services started up using `esw-services` can be interacted with using `esw-shell`, `esw-ocs-eng-ui`, or by directly
accessing the HTTP endpoints.

_NOTE: All apps/services started via `esw-services` are part of same JVM process, except Sequence Manager which is a separate
JVM process._

## Pre-requisites for running esw-services

1. The CSW services need to be running before starting the components.
   This is done by starting the `csw-services`
   If you are not building csw from the sources, you can run `csw-services` as follows:

- Install `coursier` using steps described [here](https://tmtsoftware.github.io/csw/apps/csinstallation.html) and add TMT channel.
- Run `cs install csw-services:<CSW version | SHA>`. This will create an executable file named `csw-services` in the default installation directory.
- Run `csw-services --help` to get more information.
- Run `csw-services start -c -k` to start the location service, config service and auth service.

2. Publish ESW repository locally

- Clone esw repo on your machine
- Run `sbt publishLocal` inside esw repository

## Running the esw-services using sbt

- Run `sbt "esw-services/run start"` inside esw repository.
- Run `sbt "esw-services/run start --help"` to get more information.

## Running the esw-services using coursier

- Run `cs launch esw-services:<ESW version | SHA> -- start` inside esw repository.
- Run `cs launch esw-services:<ESW version | SHA> -- start --help` to get more information.

## How to use esw-services

Without specifying any options i.e. `sbt "esw-services/run start"`, all the apps/services will be started. Which means,
two agents (`ESW.primary` and `ESW.sm_machine`), Agent service, Gateway, and Sequence Manager will be started.

_NOTE: By default, agent `ESW.sm_machine` is spawned to be used for Sequence Manager operations._

### Available options

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

_NOTE: Starting Sequence Manager automatically starts `ESW.sm_machine` agent in order to support provisioning of sequence
components. This agent is also responsible for starting Sequence Manager._

### Examples

1. Start just one agent app with prefix "TCS.machine3":

```bash
`sbt "esw-services/run start --agent --agent-prefix TCS.machine3"`
```

2. Start just gateway service with custom command role config path:

```bash
`sbt "esw-services/run start --gateway --command-role-config ~/command_role.conf"`
```

3. Start only Sequence Manager in simulation mode:

```bash
`sbt "esw-services/run start --sequence-manager --simulation"`
```

## Running the esw-services for testing esw-ocs-eng-ui

- Run `sbt "csw-services/run start -c -k"` inside csw repository.
- Run `sbt "esw-services/run start-eng-ui-services"` inside esw repository.
- Run `sbt "esw-services/run start-eng-ui-services --help"` to get more information.
- Run `npm start` inside esw-ocs-eng-ui repository.

`start-eng-ui-services` is a special command designed specifically to create a setup that would let users test out the
`esw-ocs-eng-ui` app. This sets up the following:

1. Agent `ESW.machine1`
2. Agent `AOESW.machine1`
3. Agent `IRIS.machine1`
4. Agent `TCS.machine1`
5. Agent `WFOS.machine1`
6. Agent `ESW.sm_machine` (For Sequence Manager)
7. Agent service
8. Gateway
9. Sequence Manager (in or out of simulation mode depending on the flag: sm-simulation-mode)

### Available options:

For Sequence Manager:

- `--sm-simulation-mode`: runs Sequence Manager in simulation mode.
- `--scripts-version`: specify which version of sequencer-scripts to use [optional][default version : specific sha].  

### Examples

1. Start all backend services needed for engineering UI along with sequence manager

```bash
sbt "esw-services/run start-eng-ui-services"
```

2. Start all backend services with specific sequencer-scripts version

```bash
sbt "esw-services/run start-eng-ui-services --scripts-version 0.1.0-SNAPSHOT"
```

3. Start all backend services needed for engineering UI along with sequence manager in simulation mode

```bash
sbt "esw-services/run start-eng-ui-services --sm-simulation-mode"
```

This setup ensures that all backend services needed to test out `esw-ocs-eng-ui` are up and running.
