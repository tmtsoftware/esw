addSbtPlugin("com.timushev.sbt"                  % "sbt-updates"              % "0.6.4")
addSbtPlugin("io.spray"                          % "sbt-revolver"             % "0.9.1")
addSbtPlugin("org.scoverage"                     % "sbt-scoverage"            % "2.0.6")
addSbtPlugin("com.eed3si9n"                      % "sbt-buildinfo"            % "0.11.0")
addSbtPlugin("org.scalameta"                     % "sbt-scalafmt"             % "2.5.0")
addSbtPlugin("org.portable-scala"                % "sbt-scalajs-crossproject" % "1.2.0")
addSbtPlugin("org.scala-js"                      % "sbt-scalajs"              % "1.12.0")
addSbtPlugin("com.typesafe.sbt"                  % "sbt-multi-jvm"            % "0.4.0")
addSbtPlugin("com.timushev.sbt"                  % "sbt-rewarn"               % "0.1.3")
addSbtPlugin("com.thoughtworks.sbt-api-mappings" % "sbt-api-mappings"         % "3.0.2")
addSbtPlugin("com.dwijnand"                      % "sbt-project-graph"        % "0.4.0")

ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

resolvers += "jitpack" at "https://jitpack.io"
libraryDependencies += "com.github.tmtsoftware" % "kotlin-plugin" % "24d598a"
libraryDependencies += "com.github.tmtsoftware" % "sbt-docs"      % "115000a"

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
