import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbt.Def.{setting => dep}
import sbt._

object Libs {
  private val MSocketVersion = "0.2.0"

  val `case-app`           = "com.github.alexarchambault" %% "case-app" % "2.0.4"
  val enumeratum           = dep("com.beachape" %%% "enumeratum" % "1.6.1") //MIT License
  val `mockito-scala`      = "org.mockito"                    %% "mockito-scala"      % "1.16.0" // MIT License
  val `scala-async`        = "org.scala-lang.modules" %% "scala-async" % "1.0.0-M1" //BSD 3-clause "New" or "Revised" License
  val scalatest            = dep("org.scalatest" %%% "scalatest" % "3.1.4") //Apache License 2.0
  val `scala-java8-compat` = "org.scala-lang.modules"         %% "scala-java8-compat" % "0.9.1"  //BSD 3-clause "New" or "Revised" License
  val `msocket-api`        = dep("com.github.tmtsoftware.msocket" %%% "msocket-api" % MSocketVersion)
  val `msocket-http`       = "com.github.tmtsoftware.msocket" %% "msocket-http"       % MSocketVersion
  val caffeine             = "com.github.ben-manes.caffeine"   % "caffeine"           % "2.8.6"
  val `jupiter-interface`  = "net.aichler"                     % "jupiter-interface"  % "0.8.3"
  val `tmt-test-reporter`  = "com.github.tmtsoftware"         %% "rtm"                % "33b2359b23"

  val blockhound          = "io.projectreactor.tools"                   % "blockhound"        % "1.0.4.RELEASE"
  val `embedded-keycloak` = "com.github.tmtsoftware.embedded-keycloak" %% "embedded-keycloak" % "0.2.0"

  val `ammonite` = "com.lihaoyi" % "ammonite" % "2.2.0" cross CrossVersion.full
  val `hdr-histogram`      = "org.hdrhistogram"         % "HdrHistogram"      % "2.1.12"
}

object Csw {
  private val Org = "com.github.tmtsoftware.csw"

  private val Version = "380917c24e"

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
  private val Version     = "2.6.10"
  val `akka-actor-typed`  = "com.typesafe.akka" %% "akka-actor-typed"  % Version
  val `akka-stream-typed` = "com.typesafe.akka" %% "akka-stream-typed" % Version
  val `akka-stream`       = "com.typesafe.akka" %% "akka-stream"       % Version
  val `akka-remote`       = "com.typesafe.akka" %% "akka-remote"       % Version

  val `akka-actor-testkit-typed` = "com.typesafe.akka" %% "akka-actor-testkit-typed" % Version
  val `akka-stream-testkit`      = "com.typesafe.akka" %% "akka-stream-testkit"      % Version
  val `akka-multi-node-testkit`  = "com.typesafe.akka" %% "akka-multi-node-testkit"  % Version
}

object AkkaHttp {
  private val Version = "10.2.1" //all akka is Apache License 2.0

  val `akka-http`            = "com.typesafe.akka" %% "akka-http"            % Version
  val `akka-http-testkit`    = "com.typesafe.akka" %% "akka-http-testkit"    % Version
  val `akka-http-spray-json` = "com.typesafe.akka" %% "akka-http-spray-json" % Version

  val `akka-http-cors` = "ch.megard" %% "akka-http-cors" % "1.1.0"
}

object Borer {
  private val Version = "1.6.2"
  private val Org     = "io.bullet"

  val `borer-core`        = dep(Org %%% "borer-core" % Version)
  val `borer-derivation`  = dep(Org %%% "borer-derivation" % Version)
  val `borer-compat-akka` = Org %% "borer-compat-akka" % Version
}

object Kotlin {
  val `stdlib-jdk8`     = "org.jetbrains.kotlin"  % "kotlin-stdlib-jdk8"         % "1.4.10"
  val `coroutines-jdk8` = "org.jetbrains.kotlinx" % "kotlinx-coroutines-jdk8"    % "1.4.0"
  val kotlintest        = "io.kotest"             % "kotest-assertions-core-jvm" % "4.3.1"
  val mockk             = "io.mockk"              % "mockk"                      % "1.10.2"
}
