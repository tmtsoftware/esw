TMT Executive Software (ESW)
=========================
[![Build Status](http://ec2-35-154-215-191.ap-south-1.compute.amazonaws.com:8080/job/esw-dev/badge/icon)](http://ec2-35-154-215-191.ap-south-1.compute.amazonaws.com:8080/job/esw-dev/)

## Working with the project in IntelliJ IDEA
==========================

### Working with root esw scala project
1. Clone the project
1. Import new project in intellij and select `build.sbt` file in the project's root folder
    1. This loads top level scala sbt project
1. Run `sbt $cmd` to execute required sbt tasks 

### Working with esw-integration-test module
1. First run `./build.sh` script to publish required dependencies (`esw-kt/examples:0.1.0-SNAPSHOT`) locally into `.m2`  
1. `esw-integration-test` module is not included in `aggregatedProjects` in `build.sbt`, hence top level `sbt` commands will not execute against `esw-integration-test`.
1. Run `sbt esw-integration-test/${cmd}` to execute task against `esw-integration-test` module

### Working with esw-kt kotlin project
1. First run `./build.sh` script to publish required dependencies (scala - `esw:0.1.0-SNAPSHOT`) locally into `.m2` 
1. Click on `gradle` tab present at right vertical pane in intellij
1. Click `refresh` button to reimport all gradle projects 

## Building
Run `./build.sh`
1. Publishes all scala artifacts locally to `.m2`
1. Publishes all kotlin artifacts locally to `.m2`

## Testing
Run `./test.sh`
1. Executes `build.sh` to publish required dependencies locally
1. Executes `sbt test` (scala tests)
1. Executes `gradlew test` (kotlin tests)
1. Executes `sbt esw-integration-test/test` (integration tests)