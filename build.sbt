organization in ThisBuild := "io.estatico"

name := "bson-codec"

scalacOptions ++= Seq(
  "-Xfatal-warnings",
  "-unchecked",
  "-feature",
  "-deprecation",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:experimental.macros"
)

libraryDependencies ++= Seq(
  scalaOrganization.value % "scala-reflect" % scalaVersion.value % Provided,
  scalaOrganization.value % "scala-compiler" % scalaVersion.value % Provided,
  "org.mongodb" % "mongo-java-driver" % "3.5.0",
  "org.scalatest" %% "scalatest" % "3.0.4" % "test"
)

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
