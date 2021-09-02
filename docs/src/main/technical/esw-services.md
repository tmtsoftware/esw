# esw-services

esw-services is an application that allows to start all the services offered by Executive Software on a development environment. It by default starts all the services when no option is provided. Starting specific services are possible by passing arguments corresponding to specific services.

esw-service can help users start 

* Agent app which is responsible for starting Sequence Components, Sequence Manager and Containers on different machines.

* Agent Service helps in forwarding HTTP agent related request to corresponding agents. 

* Gateway

* Sequence Manager (in Simulation Mode or without Simulation)

All the above services are part of one JVM process except Sequence Manager. 

## Pre-Requisites to run esw-services

To start esw-services the csw-services must be up and running.

To install and start csw-services please visit [here](https://tmtsoftware.github.io/csw//apps/cswservices.html)
   
## Install and start esw-services     

There are two method available to start esw-services. User can opt any one of them:

### 1. Starting esw-services using sbt

* Clone the repo locally on your machine from [here](https://github.com/tmtsoftware/esw). 
* Now go to the project directory and run `sbt publishLocal`
* To start the service run `sbt "esw-services/run start"`

The above steps will start all the app/services available in ESW. The agent ESW.sm_machine will be spawned by default for Sequence Manager operations.

### 2. Starting esw-services using coursier

* To install esw-services using coursier run `cs install esw-services:0.3.0-RC1` where 0.3.0-RC1 is the latest version. Do this once for the first time or whenever new changes are pulled from GitHub.
* To start esw-services run `cs launch esw-services:<ESW version | SHA> -- start` 

## Available options while starting esw-services

To start esw-services with only specific options append the below parameters to the command for starting esw-services mentioned above in section @ref[here](./esw-services.md#1-starting-esw-services-using-sbt) 

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

## Start Engineering UI with esw-services

One can start Engineering UI services while starting esw-services if need be by using below command inside esw directory.

`sbt "esw-services/run start-eng-ui-services"`

Above command starts all backend services like Agents for different subsystems like ESW,WFOS,TCS,IRIS,AOESW and Sequence Manager needed for Engineering UI along with Agent Service, Gateway and Sequence Manager. 

To start Engineering UI services with Sequence Manager in Simulation Mode

`sbt "esw-services/run start-eng-ui-services --sm-simulation-mode"`

To start Engineering UI services with a specific sequencer-script version

`sbt "esw-services/run start-eng-ui-services --scripts-version 0.1.0-SNAPSHOT"`


