addSbtPlugin("com.timushev.sbt"      % "sbt-updates"              % "0.5.1")
addSbtPlugin("net.virtual-void"      % "sbt-dependency-graph"     % "0.10.0-RC1")
addSbtPlugin("io.spray"              % "sbt-revolver"             % "0.9.1")
addSbtPlugin("org.scoverage"         % "sbt-scoverage"            % "1.6.1")
addSbtPlugin("com.eed3si9n"          % "sbt-buildinfo"            % "0.9.0")
addSbtPlugin("org.foundweekends"     % "sbt-bintray"              % "0.5.6")
addSbtPlugin("org.scalameta"         % "sbt-scalafmt"             % "2.4.0")
addSbtPlugin("com.dwijnand"          % "sbt-dynver"               % "4.1.1")
addSbtPlugin("org.portable-scala"    % "sbt-scalajs-crossproject" % "1.0.0")
addSbtPlugin("org.scala-js"          % "sbt-scalajs"              % "1.1.1")
addSbtPlugin("com.typesafe.sbt"      % "sbt-multi-jvm"            % "0.4.0")
addSbtPlugin("com.lightbend.paradox" % "sbt-paradox"              % "0.8.0")

resolvers += "Jenkins repo" at "https://repo.jenkins-ci.org/public/"
addSbtPlugin("ohnosequences" % "sbt-github-release" % "0.7.0")

resolvers += Resolver.bintrayRepo("twtmt", "sbt-plugins")
addSbtPlugin("com.github.tmtsoftware" % "sbt-docs"      % "0.1.5")
addSbtPlugin("com.github.tmtsoftware" % "kotlin-plugin" % "2.0.1-RC1")

resolvers += Resolver.jcenterRepo
addSbtPlugin("net.aichler" % "sbt-jupiter-interface" % "0.8.3")

libraryDependencies += "com.sun.activation" % "javax.activation" % "1.2.0"

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
