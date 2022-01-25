import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbt.Def.{setting => dep}
import sbt._

import java.io.FileReader
import java.util.Properties
import scala.util.Using

object Libs {
  private val MSocketVersion = "0.6.0"

  val `case-app`           = "com.github.alexarchambault" %% "case-app" % "2.0.6"
  val enumeratum           = dep("com.beachape" %%% "enumeratum" % "1.7.0") //MIT License
  val `mockito`            = "org.scalatestplus"              %% "mockito-3-4"        % "3.2.10.0"
  val `scala-async`        = "org.scala-lang.modules" %% "scala-async" % "1.0.1" //BSD 3-clause "New" or "Revised" License
  val scalatest            = dep("org.scalatest" %%% "scalatest" % "3.2.11") //Apache License 2.0
  val `scala-java8-compat` = "org.scala-lang.modules"         %% "scala-java8-compat" % "1.0.2" //BSD 3-clause "New" or "Revised" License
  val `msocket-api`        = dep("com.github.tmtsoftware.msocket" %%% "msocket-api" % MSocketVersion)
  val `msocket-http`       = "com.github.tmtsoftware.msocket" %% "msocket-http"       % MSocketVersion
  val caffeine             = "com.github.ben-manes.caffeine"   % "caffeine"           % "3.0.5"
  val `jupiter-interface`  = "net.aichler"                     % "jupiter-interface"  % "0.9.1"
  val `tmt-test-reporter`  = "com.github.tmtsoftware"         %% "rtm"                % "0.3.0"

  val blockhound          = "io.projectreactor.tools"                   % "blockhound"        % "1.0.6.RELEASE"
  val `embedded-keycloak` = "com.github.tmtsoftware.embedded-keycloak" %% "embedded-keycloak" % "0.6.0"

  val `ammonite` = ("com.lihaoyi" % "ammonite" % "2.5.1" cross CrossVersion.full)
    .exclude("com.lihaoyi", "sourcecode_3")
    .exclude("com.lihaoyi", "fansi_3")
    .exclude("com.lihaoyi", "pprint_3")

  val `hdr-histogram` = "org.hdrhistogram" % "HdrHistogram" % "2.1.12"
  val `slf4j-api`     = "org.slf4j"        % "slf4j-api"    % "1.7.33"

}

object Csw {
  private val Org = "com.github.tmtsoftware.csw"

  private val Version = BuildProperties.read("csw.version")

  val `csw-aas-http`        = Org %% "csw-aas-http"        % Version
  val `csw-alarm-api`       = Org %% "csw-alarm-api"       % Version
  val `csw-alarm-client`    = Org %% "csw-alarm-client"    % Version
  val `csw-command-api`     = dep(Org %%% "csw-command-api" % Version)
  val `csw-command-client`  = Org %% "csw-command-client"  % Version
  val `csw-commons`         = Org %% "csw-commons"         % Version
  val `csw-config-client`   = Org %% "csw-config-client"   % Version
  val `csw-contract`        = Org %% "csw-contract"        % Version
  val `csw-database`        = Org %% "csw-database"        % Version
  val `csw-event-api`       = Org %% "csw-event-api"       % Version
  val `csw-event-client`    = Org %% "csw-event-client"    % Version
  val `csw-framework`       = Org %% "csw-framework"       % Version
  val `csw-logging-models`  = dep(Org %%% "csw-logging-models" % Version)
  val `csw-logging-client`  = Org %% "csw-logging-client"  % Version
  val `csw-location-api`    = dep(Org %%% "csw-location-api" % Version)
  val `csw-location-client` = Org %% "csw-location-client" % Version
  val `csw-location-server` = Org %% "csw-location-server" % Version
  val `csw-network-utils`   = Org %% "csw-network-utils"   % Version
  val `csw-params`          = dep(Org %%% "csw-params" % Version)
  val `csw-prefix`          = dep(Org %%% "csw-prefix" % Version)
  val `csw-testkit`         = Org %% "csw-testkit"         % Version
  val `csw-time-scheduler`  = Org %% "csw-time-scheduler"  % Version
  val `csw-services`        = Org %% "csw-services"        % Version
}

object Akka {
  private val Version     = "2.6.18"
  val `akka-actor-typed`  = "com.typesafe.akka" %% "akka-actor-typed"  % Version
  val `akka-stream-typed` = "com.typesafe.akka" %% "akka-stream-typed" % Version
  val `akka-stream`       = "com.typesafe.akka" %% "akka-stream"       % Version
  val `akka-remote`       = "com.typesafe.akka" %% "akka-remote"       % Version

  val `akka-actor-testkit-typed` = "com.typesafe.akka" %% "akka-actor-testkit-typed" % Version
  val `akka-stream-testkit`      = "com.typesafe.akka" %% "akka-stream-testkit"      % Version
  val `akka-multi-node-testkit`  = "com.typesafe.akka" %% "akka-multi-node-testkit"  % Version
}

object AkkaHttp {
  private val Version = "10.2.7" //all akka is Apache License 2.0

  val `akka-http`            = "com.typesafe.akka" %% "akka-http"            % Version
  val `akka-http-testkit`    = "com.typesafe.akka" %% "akka-http-testkit"    % Version
  val `akka-http-spray-json` = "com.typesafe.akka" %% "akka-http-spray-json" % Version

  val `akka-http-cors` = "ch.megard" %% "akka-http-cors" % "1.1.2"
}

object Borer {
  private val Version = "1.7.2"
  private val Org     = "io.bullet"

  val `borer-core`        = dep(Org %%% "borer-core" % Version)
  val `borer-derivation`  = dep(Org %%% "borer-derivation" % Version)
  val `borer-compat-akka` = Org %% "borer-compat-akka" % Version
}

object Kotlin {
  val CoroutinesVersion = "1.6.0"

  val `stdlib-jdk8`     = "org.jetbrains.kotlin"  % "kotlin-stdlib-jdk8"      % EswKeys.kotlinVersion
  val `coroutines-core` = "org.jetbrains.kotlinx" % "kotlinx-coroutines-core" % CoroutinesVersion

  // core/jvm — additional core features available on Kotlin/JVM:
  //  - Dispatchers.IO dispatcher for blocking coroutines;
  //  - Executor.asCoroutineDispatcher extension, custom thread pools, and more.
  val `coroutines-core-jvm` = "org.jetbrains.kotlinx" % "kotlinx-coroutines-core-jvm" % CoroutinesVersion

  //JDK8 - CompletionStage.await, Guava ListenableFuture.await, and Google Play Services Task.await;
  val `coroutines-jdk8` = "org.jetbrains.kotlinx" % "kotlinx-coroutines-jdk8" % CoroutinesVersion

  val kotlintest = "io.kotest" % "kotest-assertions-core-jvm" % "5.1.0"
  val mockk      = "io.mockk"  % "mockk"                      % "1.12.2"
}

object BuildProperties {
  def read(key: String): String =
    Using.resource(new FileReader("project/build.properties")) { reader =>
      val prop = new Properties()
      prop.load(reader)
      prop.getProperty(key)
    }
}
