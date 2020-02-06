# Releasing

## Prerequisites

### Git
* Make sure git authentication works on jenkins agent by running cmd: `ssh -vT git@github.com`

### Bintray
* Make sure bintray credentials are setup properly by running cmd: `sbt bintrayWhoami`
* Make sure bintray credentials contain API key of bintray user, not password.

## Steps to release

### Release msocket
1. Release `msocket`
    There is not pieline to release msocket but only `git tag v0.0.0` and `git push origin v0.0.0` 
2. update msocket tag version in csw
    
### csw
1. Update release notes (`notes/<version>.markdown`) in `csw` repo
#### Note - The version in `notes` should be of format `v1.0.0` but while triggering the pipeline build parameter should be of format `1.0.0` 
2. Update top level `CHANGELOG.md`
3. Update top level `README.md`
4. Exclude projects from `build.sbt` which you do not want to release
5. Remove targets of newly added js projects in jenkins prod file  
6. Run `csw-prod` pipeline by providing `VERSION` number.

### esw
1. Update release notes (`notes/<version>.markdown`) in `csw` repo
#### Note - The version in `notes` should be of format `v1.0.0` but while triggering the pipeline build parameter should be of format `1.0.0` 
2. Update top level `CHANGELOG.md`
3. Update top level `README.md`
4. Exclude projects from `build.sbt` which you do not want to release
5. Remove targets of newly added js projects in jenkins prod file
6. Update `msocket` nad `csw` version in `Lib.scala`  
7. Run `esw-prod` pipeline by providing `VERSION` number.

### Release sequencer-scripts
1. Update the esw version in `build.sbt`
2. Update `README.md` for version compatibility
3. Release `sequencer-scripts` with the latest `VERSION` of esw.
    There is no pipeline to release sequencer-scripts but only `git tag v0.0.0` and `git push origin v0.0.0`

### Release csw-shell
1. Update the csw and esw version in `build.sbt`
2. Release `csw-shell` with the latest `VERSION` of csw and esw.
    There is no pipeline to release csw-shell but only `git tag v0.0.0` and `git push origin v0.0.0`

#### Note - `VERSION` tag is version number with 'v' as prefix. For eg. `v0.0.0`

### More detailed instructions

https://docs.google.com/document/d/1tK9W6NClJOB0wq3bFVEzdrYynCxES6BnPdSLNtyGMXo/edit?usp=sharing