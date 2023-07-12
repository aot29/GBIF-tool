import Dependencies._

ThisBuild / scalaVersion     := "3.3.0"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

lazy val root = (project in file("."))
  .settings(
    name := "gbif-tool",
    libraryDependencies += munit % Test
  )
libraryDependencies += "com.github.andyglow" %% "typesafe-config-scala" % "2.0.0"
libraryDependencies += "com.lihaoyi" %% "requests" % "0.8.0"
libraryDependencies += "com.typesafe.play" %% "play-json" % "2.10.0-RC9"