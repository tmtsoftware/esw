import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbt.Def.{setting => dep}
import sbt._

object Libs {
  private val SilencerVersion = "1.4.4"
  private val MSocketVersion  = "7702d8b"

  val `silencer-plugin` = "com.github.ghik" % "silencer-plugin" % SilencerVersion cross CrossVersion.full
  val `silencer-lib`    = "com.github.ghik" % "silencer-lib"    % SilencerVersion cross CrossVersion.full

  val `case-app`           = "com.github.alexarchambault" %% "case-app" % "2.0.0-M9"
  val enumeratum           = dep("com.beachape" %%% "enumeratum" % "1.5.15") //MIT License
  val `mockito-scala`      = "org.mockito" %% "mockito-scala" % "1.11.0" // MIT License
  val `scala-async`        = "org.scala-lang.modules" %% "scala-async" % "0.10.0" //BSD 3-clause "New" or "Revised" License
  val scalatest            = "org.scalatest" %% "scalatest" % "3.1.0" //Apache License 2.0
  val `scala-java8-compat` = "org.scala-lang.modules" %% "scala-java8-compat" % "0.9.0" //BSD 3-clause "New" or "Revised" License
  val `msocket-api`        = dep("com.github.tmtsoftware.msocket" %%% "msocket-api" % MSocketVersion)
  val `msocket-impl-jvm`   = "com.github.tmtsoftware.msocket" %% "msocket-impl" % MSocketVersion
  val `caffeine`           = "com.github.ben-manes.caffeine" % "caffeine" % "2.8.1"
  val `jupiter-interface`  = "net.aichler" % "jupiter-interface" % "0.8.3"

  val `prometheus-akka-http` = "com.lonelyplanet" %% "prometheus-akka-http" % "0.5.0"

  val blockhound = "io.projectreactor.tools" % "blockhound" % "1.0.2.RELEASE"
}

object Csw {
  private val Org     = "com.github.tmtsoftware.csw"
  private val Version = "4be64e3" //change this to 0.1.0-SNAPSHOT to test with local csw changes (after publishLocal)

  val `csw-admin-api`             = dep(Org %%% "csw-admin-api" % Version)
  val `csw-admin-impl`            = Org %% "csw-admin-impl" % Version
  val `csw-alarm-api`             = Org %% "csw-alarm-api" % Version
  val `csw-command-api`           = dep(Org %%% "csw-command-api" % Version)
  val `csw-prefix`                = dep(Org %%% "csw-prefix" % Version)
  val `csw-location-models`       = dep(Org %%% "csw-location-models" % Version)
  val `csw-logging-models`        = dep(Org %%% "csw-logging-models" % Version)
  val `csw-location-api`          = Org %% "csw-location-api" % Version
  val `csw-event-api`             = Org %% "csw-event-api" % Version
  val `csw-aas-http`              = Org %% "csw-aas-http" % Version
  val `csw-alarm-client`          = Org %% "csw-alarm-client" % Version
  val `csw-params`                = dep(Org %%% "csw-params" % Version)
  val `csw-commons`               = Org %% "csw-commons" % Version
  val `csw-network-utils`         = Org %% "csw-network-utils" % Version
  val `csw-location-client`       = Org %% "csw-location-client" % Version
  val `csw-location-server`       = Org %% "csw-location-server" % Version
  val `csw-command-client`        = Org %% "csw-command-client" % Version
  val `csw-event-client`          = Org %% "csw-event-client" % Version
  val `csw-time-scheduler`        = Org %% "csw-time-scheduler" % Version
  val `csw-testkit`               = Org %% "csw-testkit" % Version
  val `csw-admin-server`          = Org %% "csw-admin-server" % Version
  val `csw-config-client`         = Org %% "csw-config-client" % Version
  val `csw-database`              = Org %% "csw-database" % Version
  val `csw-contract`              = Org %% "csw-contract" % Version
  val `csw-location-server-tests` = Org %% "csw-location-server" % Version classifier "tests"
  val `csw-integration-multi-jvm` = Org %% "integration" % Version classifier "multi-jvm"
}

object Akka {
  private val Version     = "2.6.3"
  val `akka-actor-typed`  = "com.typesafe.akka" %% "akka-actor-typed" % Version
  val `akka-stream-typed` = "com.typesafe.akka" %% "akka-stream-typed" % Version
  val `akka-stream`       = "com.typesafe.akka" %% "akka-stream" % Version

  val `akka-testkit`             = "com.typesafe.akka" %% "akka-testkit"             % Version
  val `akka-actor-testkit-typed` = "com.typesafe.akka" %% "akka-actor-testkit-typed" % Version
  val `akka-stream-testkit`      = "com.typesafe.akka" %% "akka-stream-testkit"      % Version
  val `akka-multi-node-testkit`  = "com.typesafe.akka" %% "akka-multi-node-testkit"  % Version
}

object AkkaHttp {
  private val Version = "10.1.11" //all akka is Apache License 2.0

  val `akka-http`            = "com.typesafe.akka" %% "akka-http"            % Version
  val `akka-http-testkit`    = "com.typesafe.akka" %% "akka-http-testkit"    % Version
  val `akka-http-spray-json` = "com.typesafe.akka" %% "akka-http-spray-json" % Version

  val `akka-http-cors` = "ch.megard" %% "akka-http-cors" % "0.4.2"
}

object Borer {
  private val Version = "1.4.0"
  private val Org     = "io.bullet"

  val `borer-core`        = dep(Org %%% "borer-core" % Version)
  val `borer-derivation`  = dep(Org %%% "borer-derivation" % Version)
  val `borer-compat-akka` = Org %% "borer-compat-akka" % Version
}

object Kotlin {
  val `stdlib-jdk8`     = "org.jetbrains.kotlin"  % "kotlin-stdlib-jdk8"      % "1.3.61"
  val `coroutines-jdk8` = "org.jetbrains.kotlinx" % "kotlinx-coroutines-jdk8" % "1.3.3"
  val kotlintest        = "io.kotlintest"         % "kotlintest-core"         % "3.4.2"
  val mockk             = "io.mockk"              % "mockk"                   % "1.9.3"
}
