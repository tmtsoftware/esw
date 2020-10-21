# Getting and Running ESW Applications

ESW applications are installed locally using the `coursier` application, a standalone program that makes using the
JVM and Scala/Java applications easier to use and maintain. 

## Prerequisites

1. For starting and running ESW apps for development, you need to run one or more CSW services like `location-server`, `event-service`, `aas-service` etc.
Please refer to the doc @extref[here](csw:commons/apps) to run CSW services.

2. Install the coursier application

Please refer to [coursier installation document](https://get-coursier.io/docs/cli-installation)


## Getting ESW Apps

`cs install` command will be used to install executables/launchers of each of ESW apps.

For example following command shows installation for agent app,
```bash
cs install agent-app:<version | SHA>
```

Each app section explains installation and running procedure in detail.