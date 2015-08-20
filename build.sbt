name := "RSSReader"

version := "1.0"

scalaVersion := "2.11.7"

resolvers ++= Seq(
  "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
  "Typesafe Releases" at "https://repo.typesafe.com/typesafe/releases/"
)

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % "1.0.4",
  "org.scalatest" %% "scalatest" % "2.2.5" % "test",
  "org.scalactic" %% "scalactic" % "2.2.5",
  "org.reactivemongo" %% "reactivemongo" % "0.11.6",
  "org.reactivemongo" %% "play2-reactivemongo" % "0.11.6.play24",
  "org.reactivemongo" %% "reactivemongo-extensions-json" % "0.11.6.play24",
  "com.typesafe.play" %% "play-json" % "2.4.2",
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0"
)

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-dead-code"
  //"-Ywarn-numeric-widen"
  //"-Ywarn-value-discard"
)