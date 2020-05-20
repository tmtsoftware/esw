# Releasing

## Prerequisites (This is configured in `release.yml`)

* Git authentication works by running cmd: `ssh -vT git@github.com`
* Node is installed
* npm module `junit-merge` is installed (for merging multiple xml test reports into one)
* npm module `junit-viewer` is installed (for generating html test report from merged xml)

## Steps to release

### Release msocket

Refer RELEASING.md in `msocket`

### Release csw

Refer RELEASING.md in `csw` repository.

### esw

1. Update release notes (`notes/<version>.markdown`) in `csw` repo
    **Note** - The version in `notes` should be of format `v1.0.0`
1. Update top-level `CHANGELOG.md`
1. Update top-level `README.md`
1. Exclude projects from `build.sbt` which you do not want to release
1. Update `msocket` and `csw` version in `Lib.scala`  
1. Run `release.sh $VERSION$` script by providing version number argument (This triggers release workflow)

    **Note:** `PROD=true` environment variable needs to be set before running `release.sh`

### Release sequencer-scripts

Refer RELEASING.md in `sequencer-scripts` repository.
