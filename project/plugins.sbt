addSbtPlugin("io.spray"           % "sbt-revolver"               % "0.9.1")
addSbtPlugin("com.timushev.sbt"   % "sbt-updates"                % "0.4.2")
addSbtPlugin("org.scoverage"      % "sbt-scoverage"              % "1.6.0")
addSbtPlugin("com.typesafe.sbt"   % "sbt-native-packager"        % "1.4.1")
addSbtPlugin("com.eed3si9n"       % "sbt-buildinfo"              % "0.9.0")
addSbtPlugin("com.typesafe.sbt"   % "sbt-ghpages"                % "0.6.3")
addSbtPlugin("org.foundweekends"  % "sbt-bintray"                % "0.5.5")
addSbtPlugin("io.github.jonas"    % "sbt-paradox-material-theme" % "0.6.0")
addSbtPlugin("com.eed3si9n"       % "sbt-unidoc"                 % "0.4.2")
addSbtPlugin("com.typesafe.sbt"   % "sbt-site"                   % "1.3.3")
addSbtPlugin("org.scalameta"      % "sbt-scalafmt"               % "2.0.6")
addSbtPlugin("com.typesafe.sbt"   % "sbt-ghpages"                % "0.6.3")
addSbtPlugin("com.typesafe.sbt"   % "sbt-git"                    % "1.0.0")
addSbtPlugin("com.dwijnand"       % "sbt-dynver"                 % "4.0.0")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject"   % "0.6.1")
addSbtPlugin("org.scala-js"       % "sbt-scalajs"                % "0.6.29")

resolvers += "Jenkins repo" at "https://repo.jenkins-ci.org/public/"
addSbtPlugin("ohnosequences" % "sbt-github-release" % "0.7.0")

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
