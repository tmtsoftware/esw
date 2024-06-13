import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbt.Def.{setting => dep}
import sbt._

import java.io.FileReader
import java.util.Properties
import scala.util.Using

object Libs {

  val `case-app`          = "com.github.alexarchambault"   %% "case-app"          % "2.1.0-M26"
  val `enumeratum`        = dep("com.beachape" %%% "enumeratum" % "1.7.3") // MIT License
  val `mockito`           = "org.scalatestplus"            %% "mockito-3-4"       % "3.2.10.0"
  val `dotty-cps-async`   = dep("com.github.rssh" %%% "dotty-cps-async" % "0.9.21")
  val `scalatest`         = dep("org.scalatest" %%% "scalatest" % "3.2.18") // Apache License 2.0
  val `caffeine`          = "com.github.ben-manes.caffeine" % "caffeine"          % "3.1.8"
  val `jupiter-interface` = "net.aichler"                   % "jupiter-interface" % "0.11.1"
  val `tmt-test-reporter` = "com.github.tmtsoftware.rtm"   %% "rtm"               % "b7997a9"

  val blockhound          = "io.projectreactor.tools"                   % "blockhound"        % "1.0.8.RELEASE"
  val `embedded-keycloak` = "com.github.tmtsoftware.embedded-keycloak" %% "embedded-keycloak" % "2572711" // Apache 2.0

  // Note: CrossVersion.full: version has to match exact scala version (_3.3.0 instead of _3)
  val `ammonite` = ("com.lihaoyi" % "ammonite_3.3.3" % "3.0.0-M2-6-38698450")

  val `hdr-histogram` = "org.hdrhistogram"   % "HdrHistogram" % "2.1.12"
  val `slf4j-api`     = "org.slf4j"          % "slf4j-api"    % "2.0.7"
  val `play-json`     = "org.playframework" %% "play-json"    % "3.0.3" // Apache 2.0
  val scopt           = "com.github.scopt"  %% "scopt"        % "4.1.0" // MIT License
}

object MSocket {
  //  val Version = "0.6.0"
  val Version = "06b7251"

  val `msocket-api`  = dep("com.github.tmtsoftware.msocket" %%% "msocket-api" % Version)
  val `msocket-http` = "com.github.tmtsoftware.msocket" %% "msocket-http" % Version
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

object Pekko {
  val Version = "1.0.2" // all pekko is Apache License 2.0
  val Org     = "org.apache.pekko"

  val `pekko-actor-typed`  = Org %% "pekko-actor-typed"  % Version
  val `pekko-stream-typed` = Org %% "pekko-stream-typed" % Version
  val `pekko-stream`       = Org %% "pekko-stream"       % Version
  val `pekko-remote`       = Org %% "pekko-remote"       % Version

  val `pekko-actor-testkit-typed` = Org %% "pekko-actor-testkit-typed" % Version
  val `pekko-stream-testkit`      = Org %% "pekko-stream-testkit"      % Version
  val `pekko-multi-node-testkit`  = Org %% "pekko-multi-node-testkit"  % Version
}

object PekkoHttp {
  val Version = "1.0.1"
  val Org     = "org.apache.pekko"

  val `pekko-http`            = Org %% "pekko-http"            % Version
  val `pekko-http-testkit`    = Org %% "pekko-http-testkit"    % Version
  val `pekko-http-spray-json` = Org %% "pekko-http-spray-json" % Version

  val `pekko-http-cors` = Org %% "pekko-http-cors" % Version
}

object Borer {
  val Version = "1.14.0"
  val Org     = "io.bullet"
  //  val Org = "com.github.tmtsoftware.borer"

  val `borer-core`         = dep(Org %%% "borer-core" % Version)
  val `borer-derivation`   = dep(Org %%% "borer-derivation" % Version)
  val `borer-compat-pekko` = Org %% "borer-compat-pekko" % Version
}

object Kotlin {
  val CoroutinesVersion = "1.7.3"

  val `stdlib-jdk8`     = "org.jetbrains.kotlin"  % "kotlin-stdlib-jdk8"      % EswKeys.kotlinVersion
  val `coroutines-core` = "org.jetbrains.kotlinx" % "kotlinx-coroutines-core" % CoroutinesVersion

  // core/jvm — additional core features available on Kotlin/JVM:
  //  - Dispatchers.IO dispatcher for blocking coroutines;
  //  - Executor.asCoroutineDispatcher extension, custom thread pools, and more.
  val `coroutines-core-jvm` = "org.jetbrains.kotlinx" % "kotlinx-coroutines-core-jvm" % CoroutinesVersion

  // JDK8 - CompletionStage.await, Guava ListenableFuture.await, and Google Play Services Task.await;
  val `coroutines-jdk8` = "org.jetbrains.kotlinx" % "kotlinx-coroutines-jdk8" % CoroutinesVersion

  val kotlintest = "io.kotest" % "kotest-assertions-core-jvm" % "5.8.0"
  val mockk      = "io.mockk"  % "mockk-jvm"                  % "1.13.9"
}

object Http4k {
  val Version                = "5.23.0.0"
  val `http4k-core`          = "org.http4k" % "http4k-core"          % Version
  val `http4k-server-jetty`  = "org.http4k" % "http4k-server-jetty"  % Version
  val `http4k-client-okhttp` = "org.http4k" % "http4k-client-okhttp" % Version
}

object BuildProperties {
  def read(key: String): String =
    Using.resource(new FileReader("project/build.properties")) { reader =>
      val prop = new Properties()
      prop.load(reader)
      prop.getProperty(key)
    }
}
