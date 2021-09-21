# Instructions

Running esw-gateway-server on local machines requires
that you start location service on your local machine.

Before you start location service, ensure that it's build
from sha which matches `csw.version` in `project/build.properties`
in this repo.

Start needed CSW services

```bash
cs launch csw-services:<csw.version in project/build.properties> -- start
```

Note: Detail documentation to start csw-services can be found [here](https://tmtsoftware.github.io/csw//apps/cswservices.html) 

Once these services are started, esw gateway server can be started with 

```bash
sbt "esw-gateway-server/run start"
```
