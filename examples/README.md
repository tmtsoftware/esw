
## How to run script

### Prerequisite:
- `csw-services.sh` script running

### Running script
- In application.conf file, add a new entry for script class inside sequencer id and observing mode.
Ex.
```
scripts {
  iris {
    darknight {
      scriptClass = esw.ocs.scripts.examples.script_based.Script3
      prefix = iris.ocs.prefix1
    }
  }
}
```
- Then run following command to start `sequencer` in standalone mode 
```./gradlew examples:run --args="sequencer --id iris --mode darknight"```
   
 