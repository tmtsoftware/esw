import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbt.Def.{setting => dep}
import sbt._

object Libs {
  private val ScalaVersion: String = EswKeys.scalaVersion
  private val SilencerVersion      = "1.4.2"
  private val MSocketVersion       = "568f59d"

  // fixme: find appropriate place to keep kotlin test scripts used in integration-test project
  val `examples` = "com.github.tmtsoftware.script-dsl" % "examples" % "e687b1d155"

  val `scala-reflect`      = "org.scala-lang" % "scala-reflect" % ScalaVersion
  val `silencer-lib`       = "com.github.ghik" %% "silencer-lib" % SilencerVersion % Compile
  val `silencer-plugin`    = compilerPlugin("com.github.ghik" %% "silencer-plugin" % SilencerVersion)
  val `case-app`           = "com.github.alexarchambault" %% "case-app" % "2.0.0-M9"
  val enumeratum           = dep("com.beachape" %%% "enumeratum" % "1.5.13") //MIT License
  val `mockito-scala`      = "org.mockito" %% "mockito-scala" % "1.5.17" // MIT License
  val `scala-async`        = "org.scala-lang.modules" %% "scala-async" % "0.10.0" //BSD 3-clause "New" or "Revised" License
  val scopt                = "com.github.scopt" %% "scopt" % "4.0.0-RC2" //MIT License
  val scalatest            = "org.scalatest" %% "scalatest" % "3.0.8" //Apache License 2.0
  val `scala-java8-compat` = "org.scala-lang.modules" %% "scala-java8-compat" % "0.9.0" //BSD 3-clause "New" or "Revised" License
  val `msocket-api`        = dep("com.github.tmtsoftware.msocket" %%% "msocket-api" % MSocketVersion)
  val `msocket-impl-jvm`   = "com.github.tmtsoftware.msocket" %% "msocket-impl-jvm" % MSocketVersion
}

object Csw {
  private val Org     = "com.github.tmtsoftware.csw"
  private val Version = "bb5dffed43" //change this to 0.1-SNAPSHOT to test with local csw changes (after publishLocal)

  val `csw-alarm-api`       = Org %% "csw-alarm-api" % Version
  val `csw-command-api`     = Org %% "csw-command-api" % Version
  val `csw-location-models` = dep(Org %%% "csw-location-models" % Version)
  val `csw-logging-models`  = Org %% "csw-logging-models" % Version
  val `csw-location-api`    = Org %% "csw-location-api" % Version
  val `csw-event-api`       = Org %% "csw-event-api" % Version
  val `csw-aas-http`        = Org %% "csw-aas-http" % Version
  val `csw-alarm-client`    = Org %% "csw-alarm-client" % Version
  val `csw-params`          = dep(Org %%% "csw-params" % Version)
  val `csw-commons`         = Org %% "csw-commons" % Version
  val `csw-network-utils`   = Org %% "csw-network-utils" % Version
  val `csw-location-client` = Org %% "csw-location-client" % Version
  val `csw-command-client`  = Org %% "csw-command-client" % Version
  val `csw-event-client`    = Org %% "csw-event-client" % Version
  val `csw-time-scheduler`  = Org %% "csw-time-scheduler" % Version
  val `csw-testkit`         = Org %% "csw-testkit" % Version
  val `csw-admin-server`    = Org %% "csw-admin-server" % Version
  val `csw-config-client`   = Org %% "csw-config-client" % Version
}

object Akka {
  private val Version     = "2.5.25"
  val `akka-actor-typed`  = "com.typesafe.akka" %% "akka-actor-typed" % Version
  val `akka-stream-typed` = "com.typesafe.akka" %% "akka-stream-typed" % Version
  val `akka-stream`       = "com.typesafe.akka" %% "akka-stream" % Version

  val `akka-testkit`             = "com.typesafe.akka" %% "akka-testkit"             % Version
  val `akka-actor-testkit-typed` = "com.typesafe.akka" %% "akka-actor-testkit-typed" % Version
  val `akka-stream-testkit`      = "com.typesafe.akka" %% "akka-stream-testkit"      % Version
}

object AkkaHttp {
  private val Version = "10.1.10" //all akka is Apache License 2.0

  val `akka-http`         = "com.typesafe.akka" %% "akka-http"         % Version
  val `akka-http-testkit` = "com.typesafe.akka" %% "akka-http-testkit" % Version
  val `akka-http-cors`    = "ch.megard"         %% "akka-http-cors"    % "0.4.1"
}

object Borer {
  private val Version = "1.0.0"
  private val Org     = "io.bullet"

  val `borer-core`        = Org %% "borer-core"        % Version
  val `borer-derivation`  = Org %% "borer-derivation"  % Version
  val `borer-compat-akka` = Org %% "borer-compat-akka" % Version
}
