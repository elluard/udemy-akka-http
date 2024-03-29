ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

val akkaVersion = "2.6.18"
val akkaHttpVersion = "10.2.7"
val scalaTestVersion = "3.2.9"

lazy val root = (project in file("."))
  .settings(
    name := "udemy-akka-http",
    libraryDependencies ++= Seq(
      // akka streams
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      // akka http
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion,
      // testing
      "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
      "org.scalatest" %% "scalatest" % scalaTestVersion,

      // JWT
      "com.pauldijou" %% "jwt-spray-json" % "5.0.0"

    )
  )
