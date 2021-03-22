# esw-services

## Pre-requisites

1. The CSW services need to be running before starting the components.
This is done by starting the `csw-services`
If you are not building csw from the sources, you can run `csw-services` as follows:

- Install `coursier` using steps described [here](https://tmtsoftware.github.io/csw/apps/csinstallation.html) and add TMT channel.
- Run `cs install csw-services:<CSW version | SHA>`. This will create an executable file named `csw-services` in the default installation directory.
- Run `csw-services --help` to get more information.
- Run `csw-services start -c -k` to start the location service, config service and auth service.

1. Publish local in esw repository
- Clone esw repo on your machine
- Run `sbt publishLocal` inside esw repository


## Running the esw-services using sbt

- Run `sbt "esw-services/run start"` inside esw repository.
- Run `sbt "esw-services/run start --help"` to get more information.

## Running the esw-services for testing esw-ocs-eng-ui

- Run `sbt "csw-services/run start -c -k"` inside csw repository.
- Run `sbt "esw-services/run start-eng-ui-services"` inside esw repository.
- Run `npm start` inside esw-ocs-eng-ui repository.
