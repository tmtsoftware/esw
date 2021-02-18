1. start location service
cs launch location-server:2ce0a752bd -- --clusterPort=3552

1. start SM
sbt
project esw-sm-app
run start -o /Users/in-poorvag/TMT/esw/esw-performance-test/src/main/resources/smObsModes.conf --simulation

1. start seq comp
sbt
project esw-ocs-app
run seqcomp -s ESW -n primary --simulation

1. run test
sbt
project esw-performance-test
runMain esw.performance.SequenceManagerReliabilityTest

