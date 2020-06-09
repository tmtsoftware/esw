import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbt.Def.{setting => dep}
import sbt._

object Libs {
  private val MSocketVersion = "aad3e260a4"

  val `case-app`           = "com.github.alexarchambault" %% "case-app" % "2.0.0-M16"
  val enumeratum           = dep("com.beachape" %%% "enumeratum" % "1.6.1") //MIT License
  val `mockito-scala`      = "org.mockito"                    %% "mockito-scala"      % "1.14.3" // MIT License
  val `scala-async`        = "org.scala-lang.modules" %% "scala-async" % "0.10.0" //BSD 3-clause "New" or "Revised" License
  val scalatest            = dep("org.scalatest" %%% "scalatest" % "3.1.2") //Apache License 2.0
  val `scala-java8-compat` = "org.scala-lang.modules"         %% "scala-java8-compat" % "0.9.1"  //BSD 3-clause "New" or "Revised" License
  val `msocket-api`        = dep("com.github.tmtsoftware.msocket" %%% "msocket-api" % MSocketVersion)
  val `msocket-impl-jvm`   = "com.github.tmtsoftware.msocket" %% "msocket-impl"       % MSocketVersion
  val caffeine             = "com.github.ben-manes.caffeine"   % "caffeine"           % "2.8.4"
  val `jupiter-interface`  = "net.aichler"                     % "jupiter-interface"  % "0.8.3"
  val `tmt-test-reporter`  = "com.github.tmtsoftware"         %% "rtm"                % "d1c8c7e"

  val blockhound          = "io.projectreactor.tools"                   % "blockhound"        % "1.0.3.RELEASE"
  val `embedded-keycloak` = "com.github.tmtsoftware.embedded-keycloak" %% "embedded-keycloak" % "9374d69"

  val `ammonite` = "com.lihaoyi" % "ammonite" % "2.1.4" cross CrossVersion.full
}

object Csw {
  private val Org     = "com.github.tmtsoftware.csw"
  private val Version = "8a94c01" // Change this to 0.1.0-SNAPSHOT to test with local csw changes (after publishLocal)

  val `csw-aas-http`              = Org %% "csw-aas-http"        % Version
  val `csw-admin-api`             = dep(Org %%% "csw-admin-api" % Version)
  val `csw-admin-impl`            = Org %% "csw-admin-impl"      % Version
  val `csw-admin-server`          = Org %% "csw-admin-server"    % Version
  val `csw-alarm-api`             = Org %% "csw-alarm-api"       % Version
  val `csw-alarm-client`          = Org %% "csw-alarm-client"    % Version
  val `csw-command-api`           = dep(Org %%% "csw-command-api" % Version)
  val `csw-command-client`        = Org %% "csw-command-client"  % Version
  val `csw-commons`               = Org %% "csw-commons"         % Version
  val `csw-config-client`         = Org %% "csw-config-client"   % Version
  val `csw-contract`              = Org %% "csw-contract"        % Version
  val `csw-database`              = Org %% "csw-database"        % Version
  val `csw-event-api`             = Org %% "csw-event-api"       % Version
  val `csw-event-client`          = Org %% "csw-event-client"    % Version
  val `csw-framework`             = Org %% "csw-framework"       % Version
  val `csw-integration-multi-jvm` = Org %% "integration"         % Version classifier "multi-jvm"
  val `csw-logging-models`        = dep(Org %%% "csw-logging-models" % Version)
  val `csw-location-api`          = dep(Org %%% "csw-location-api" % Version)
  val `csw-location-client`       = Org %% "csw-location-client" % Version
  val `csw-location-server`       = Org %% "csw-location-server" % Version
  val `csw-network-utils`         = Org %% "csw-network-utils"   % Version
  val `csw-params`                = dep(Org %%% "csw-params" % Version)
  val `csw-prefix`                = dep(Org %%% "csw-prefix" % Version)
  val `csw-testkit`               = Org %% "csw-testkit"         % Version
  val `csw-time-scheduler`        = Org %% "csw-time-scheduler"  % Version
}

object Akka {
  private val Version     = "2.6.5"
  val `akka-actor-typed`  = "com.typesafe.akka" %% "akka-actor-typed"  % Version
  val `akka-stream-typed` = "com.typesafe.akka" %% "akka-stream-typed" % Version
  val `akka-stream`       = "com.typesafe.akka" %% "akka-stream"       % Version
  val `akka-remote`       = "com.typesafe.akka" %% "akka-remote"       % Version

  val `akka-actor-testkit-typed` = "com.typesafe.akka" %% "akka-actor-testkit-typed" % Version
  val `akka-stream-testkit`      = "com.typesafe.akka" %% "akka-stream-testkit"      % Version
  val `akka-multi-node-testkit`  = "com.typesafe.akka" %% "akka-multi-node-testkit"  % Version
}

object AkkaHttp {
  private val Version = "10.2.0-M1" //all akka is Apache License 2.0

  val `akka-http`         = "com.typesafe.akka" %% "akka-http"         % Version
  val `akka-http-testkit` = "com.typesafe.akka" %% "akka-http-testkit" % Version

  val `akka-http-cors` = "ch.megard" %% "akka-http-cors" % "0.4.3"
}

object Borer {
  private val Version = "1.6.0"
  private val Org     = "io.bullet"

  val `borer-core`        = dep(Org %%% "borer-core" % Version)
  val `borer-derivation`  = dep(Org %%% "borer-derivation" % Version)
  val `borer-compat-akka` = Org %% "borer-compat-akka" % Version
}

object Kotlin {
  val `stdlib-jdk8`     = "org.jetbrains.kotlin"  % "kotlin-stdlib-jdk8"      % "1.3.72"
  val `coroutines-jdk8` = "org.jetbrains.kotlinx" % "kotlinx-coroutines-jdk8" % "1.3.7"
  val kotlintest        = "io.kotlintest"         % "kotlintest-core"         % "3.4.2"
  val mockk             = "io.mockk"              % "mockk"                   % "1.10.0"
}
