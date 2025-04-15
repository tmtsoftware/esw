addSbtPlugin("com.timushev.sbt"                  % "sbt-updates"               % "0.6.4")
addSbtPlugin("io.spray"                          % "sbt-revolver"              % "0.10.0")
addSbtPlugin("org.scoverage"                     % "sbt-scoverage"             % "2.3.1")
addSbtPlugin("com.eed3si9n"                      % "sbt-buildinfo"             % "0.13.1")
addSbtPlugin("org.scalameta"                     % "sbt-scalafmt"              % "2.5.4")
addSbtPlugin("org.portable-scala"                % "sbt-scalajs-crossproject"  % "1.3.2")
addSbtPlugin("org.scala-js"                      % "sbt-scalajs"               % "1.18.2")
addSbtPlugin("com.typesafe.sbt"                  % "sbt-multi-jvm"             % "0.4.0")
addSbtPlugin("com.timushev.sbt"                  % "sbt-rewarn"                % "0.1.3")
//addSbtPlugin("com.thoughtworks.sbt-api-mappings" % "sbt-api-mappings"          % "3.0.2")
addSbtPlugin("com.dwijnand"                      % "sbt-project-graph"        % "0.4.0")

ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

resolvers += "jitpack" at "https://jitpack.io"

addSbtPlugin("org.jetbrains.scala" % "sbt-kotlin-plugin" % "3.1.4")

libraryDependencies += "com.github.tmtsoftware" % "sbt-docs" % "0.7.1"

resolvers += Resolver.jcenterRepo
addSbtPlugin("net.aichler" % "sbt-jupiter-interface" % "0.11.1")

scalacOptions ++= Seq(
  "-encoding",
  "UTF-8",
  "-feature",
  "-unchecked",
  "-deprecation",
  "-Xlint:-unused,_",
  "-Ywarn-dead-code",
  "-Xfuture"
)

classpathTypes += "maven-plugin"
