# Instructions

Running esw-gateway-server on local machines requires
that you start location service on your local machine.

Before you start location service, ensure that it's build
from sha which matches `Csw.Version` in `project/Libs`
in this repo.

A good idea is to run following commands in `csw` repo

```bash
cd csw
git checkout [SHA]
sbt "universal:stage"
cd target/universal/stage/bin/
./csw-services.sh start -i en0
```

Once these services are started, esw gateway server can be started with 

```bash
sbt "esw-gateway-server/run start"
```