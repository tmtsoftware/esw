# Releasing

## Prerequisites

### Git
* Make sure git authentication works on jenkins agent by running cmd: `ssh -vT git@github.com`

## Steps to release

### Release msocket
Refer RELEASING.md in `msocket`
    
### Release csw
Refer RELEASING.md in `csw` repository.

### esw
1. Update release notes (`notes/<version>.markdown`) in `csw` repo
#### Note - The version in `notes` should be of format `v1.0.0` but while triggering the pipeline build parameter should be of format `1.0.0` 
2. Update top-level `CHANGELOG.md`
3. Update top-level `README.md`
4. Exclude projects from `build.sbt` which you do not want to release
5. Remove targets of newly added js projects in jenkins prod file
6. Update `msocket` and `csw` version in `Lib.scala`  
7. Run `esw-prod` pipeline by providing `VERSION` number.

### Release sequencer-scripts
Refer RELEASING.md in `sequencer-scripts` repository.

### Release csw-shell
Refer RELEASING.md in `csw-shell` repository.

#### Note - `VERSION` tag is version number with 'v' as prefix. For eg. `v0.0.0`

### More detailed instructions

https://docs.google.com/document/d/1tK9W6NClJOB0wq3bFVEzdrYynCxES6BnPdSLNtyGMXo/edit?usp=sharing