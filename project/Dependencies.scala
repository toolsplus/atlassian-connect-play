import sbt._

object Dependencies {
  val core = Seq(
    Library.atlassianJwt,
    Library.cats,
    Library.circeCore,
    Library.circeGeneric,
    Library.circeParser,
    Library.circePlay,
    Library.scalaUri,
    Library.scalaTest % "test",
    Library.scalaTestPlusPlay % "test",
    Library.scalaMock % "test",
    Library.scalaCheck % "test",
    Library.atlassianJwtGenerators % "test",
    Library.akkaTestKit % "test"
  )
}

object Version {
  val atlassianJwt = "0.1.7"
  val cats = "1.6.0"
  val circe = "0.11.1"
  val playCirce = "2711.0"
  val scalaUri = "0.4.16"
  val scalaTest = "3.0.5"
  val scalaTestPlusPlay = "4.0.2"
  val scalaMock = "4.2.0"
  val scalaCheck = "1.14.0"
  val akkaTestKit = "2.5.22"
}

object Library {
  val atlassianJwtGenerators = "io.toolsplus" %% "atlassian-jwt-generators" % Version.atlassianJwt
  val atlassianJwt = "io.toolsplus" %% "atlassian-jwt-core" % Version.atlassianJwt
  val cats = "org.typelevel" %% "cats-core" % Version.cats
  val circeCore = "io.circe" %% "circe-core" % Version.circe
  val circeGeneric = "io.circe" %% "circe-generic" % Version.circe
  val circeParser = "io.circe" %% "circe-parser" % Version.circe
  val circePlay = "com.dripower" %% "play-circe" % Version.playCirce
  val scalaUri = "com.netaporter" %% "scala-uri" % Version.scalaUri
  val scalaTest = "org.scalatest" %% "scalatest" % Version.scalaTest
  val scalaTestPlusPlay = "org.scalatestplus.play" %% "scalatestplus-play" % Version.scalaTestPlusPlay
  val scalaMock = "org.scalamock" %% "scalamock" % Version.scalaMock
  val scalaCheck = "org.scalacheck" %% "scalacheck" % Version.scalaCheck
  val akkaTestKit = "com.typesafe.akka" %% "akka-testkit" % Version.akkaTestKit
}
