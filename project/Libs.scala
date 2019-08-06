import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbt.Def.{setting => dep}
import sbt._

object Libs {
  val ScalaVersion: String = EswKeys.scalaVersion
  val silencerVersion      = "1.4.2"

  val `scala-reflect`   = "org.scala-lang" % "scala-reflect" % ScalaVersion
  val scalatest         = "org.scalatest" %% "scalatest" % "3.0.8" //Apache License 2.0
  val `scopt`           = "com.github.scopt" %% "scopt" % "4.0.0-RC2" //MIT License
  val `scala-async`     = "org.scala-lang.modules" %% "scala-async" % "0.10.0" //BSD 3-clause "New" or "Revised" License
  val `mockito-scala`   = "org.mockito" %% "mockito-scala" % "1.5.13" // MIT License
  val `enumeratum`      = dep("com.beachape" %%% "enumeratum" % "1.5.13") //MIT License
  val `case-app`        = "com.github.alexarchambault" %% "case-app" % "2.0.0-M9"
  val `silencer-plugin` = compilerPlugin("com.github.ghik" %% "silencer-plugin" % silencerVersion)
  val `silencer-lib`    = "com.github.ghik" %% "silencer-lib" % silencerVersion % Compile
}

object Csw {
  private val Org     = "com.github.tmtsoftware.csw"
  private val Version = "1b88179342d1b1464b58c023320697ba3d90f217" //change this to 0.1-SNAPSHOT to test with local csw changes (after publishLocal)

  val `csw-location-api`    = Org %% "csw-location-api" % Version
  val `csw-aas-http`        = Org %% "csw-aas-http" % Version
  val `csw-alarm-client`    = Org %% "csw-alarm-client" % Version
  val `csw-params`          = dep(Org %%% "csw-params" % Version)
  val `csw-commons`         = Org %% "csw-commons" % Version
  val `csw-network-utils`   = Org %% "csw-network-utils" % Version
  val `csw-location-client` = Org %% "csw-location-client" % Version
  val `csw-command-client`  = Org %% "csw-command-client" % Version
  val `csw-event-client`    = Org %% "csw-event-client" % Version
  val `csw-testkit`         = Org %% "csw-testkit" % Version
}

object Akka {
  val Version             = "2.5.23"
  val `akka-actor-typed`  = "com.typesafe.akka" %% "akka-actor-typed" % Version
  val `akka-stream-typed` = "com.typesafe.akka" %% "akka-stream-typed" % Version

  val `akka-testkit`             = "com.typesafe.akka" %% "akka-testkit"             % Version
  val `akka-actor-testkit-typed` = "com.typesafe.akka" %% "akka-actor-testkit-typed" % Version
  val `akka-stream-testkit`      = "com.typesafe.akka" %% "akka-stream-testkit"      % Version
}

object AkkaHttp {
  private val Version = "10.1.9" //all akka is Apache License 2.0

  val `akka-http`         = "com.typesafe.akka" %% "akka-http"         % Version
  val `akka-http-testkit` = "com.typesafe.akka" %% "akka-http-testkit" % Version
}
