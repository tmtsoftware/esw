addSbtPlugin("com.timushev.sbt"                  % "sbt-updates"              % "0.5.3")
addSbtPlugin("io.spray"                          % "sbt-revolver"             % "0.9.1")
addSbtPlugin("org.scoverage"                     % "sbt-scoverage"            % "1.8.2")
addSbtPlugin("com.eed3si9n"                      % "sbt-buildinfo"            % "0.10.0")
addSbtPlugin("org.scalameta"                     % "sbt-scalafmt"             % "2.4.2")
addSbtPlugin("com.dwijnand"                      % "sbt-dynver"               % "4.1.1")
addSbtPlugin("org.portable-scala"                % "sbt-scalajs-crossproject" % "1.1.0")
addSbtPlugin("org.scala-js"                      % "sbt-scalajs"              % "1.5.1")
addSbtPlugin("com.typesafe.sbt"                  % "sbt-multi-jvm"            % "0.4.0")
addSbtPlugin("com.timushev.sbt"                  % "sbt-rewarn"               % "0.1.3")
addSbtPlugin("com.thoughtworks.sbt-api-mappings" % "sbt-api-mappings"         % "3.0.0")

resolvers += "jitpack" at "https://jitpack.io"
libraryDependencies += "com.github.tmtsoftware" % "kotlin-plugin" % "3.0.0"
libraryDependencies += "com.github.tmtsoftware" % "sbt-docs"      % "0.3.0"

resolvers += Resolver.jcenterRepo
addSbtPlugin("net.aichler" % "sbt-jupiter-interface" % "0.9.1")

scalacOptions ++= Seq(
  "-encoding",
  "UTF-8",
  "-feature",
  "-unchecked",
  "-deprecation",
  //"-Xfatal-warnings",
  "-Xlint:-unused,_",
  "-Ywarn-dead-code",
  "-Xfuture"
)
