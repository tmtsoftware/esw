# Creating and Using the UI Backend Template

In this tutorial, you’ll see how to create a Scala/Java project using a [giter8](http://www.foundweekends.org/giter8/) template for ESW ([
esw-web-app-template.g8](https://github.com/tmtsoftware/esw-web-app-template.g8)) which contains sample application make using ESW library.
You can use this as a starting point for your own projects.

## Installation
Supported Operating Systems are: CentOS and MacOS

1. Make sure you have the Java AdoptOpenJDK 11.
    - Run  `javac -version`  in the command line and make sure you see  `javac 11._._`
    - If you don’t have version 11 or higher, links given below will help you to reach there.
    - [Mac](https://github.com/AdoptOpenJDK/homebrew-openjdk)
    - [Linux](https://adoptopenjdk.net/installation.html?variant=openjdk11&jvmVariant=hotspot)
2. Install sbt
    - [Mac](https://www.scala-sbt.org/1.x/docs/Installing-sbt-on-Mac.html)
    - [Linux](https://www.scala-sbt.org/1.x/docs/Installing-sbt-on-Linux.html)
3. Install IntelliJ
	- [MAC](https://www.jetbrains.com/idea/download/)
	- [Linux](https://www.jetbrains.com/idea/download/)
4. Install following IntelliJ Plugins
    - [Scala](https://plugins.jetbrains.com/plugin/1347-scala)
    - [Scalafmt](https://plugins.jetbrains.com/plugin/8236-scalafmt)
5. Recommended testing frameworks/tools:
	- [ScalaTest](https://www.scalatest.org/)
	- [JUnit](https://junit.org/junit4/), JUnit Interface
	- Note: these dependencies are specified by default in `esw-web-app-template.g8`, and the sbt will resolve them when it runs.


## Create Starter Project
1. Follow the template [readme.md](https://github.com/tmtsoftware/esw-web-app-template.g8/blob/master/README.md) for detailed information about project creation.
