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
    Library.sttp,
    Library.scalaTest % "test",
    Library.scalaTestPlusScalaCheck % "test",
    Library.scalaTestPlusPlay % "test",
    Library.scalaMock % "test",
    Library.scalaCheck % "test",
    Library.atlassianJwtGenerators % "test",
    Library.akkaTestKit % "test"
  )
}

object Version {
  val atlassianJwt = "0.2.1"
  val cats = "2.9.0"
  val circe = "0.14.5"
  val playCirce = "2814.4"
  val scalaUri = "4.0.3"
  val sttp = "3.9.1"
  val scalaTest = "3.2.9"
  val scalaTestPlusPlay = "5.1.0"
  val scalaTestPlusScalaCheck = "3.2.9.0"
  val scalaMock = "5.1.0"
  val scalaCheck = "1.15.4"
  val akkaTestKit = "2.6.21"
}

object Library {
  val atlassianJwtGenerators = "io.toolsplus" %% "atlassian-jwt-generators" % Version.atlassianJwt
  val atlassianJwt = "io.toolsplus" %% "atlassian-jwt-core" % Version.atlassianJwt
  val cats = "org.typelevel" %% "cats-core" % Version.cats
  val circeCore = "io.circe" %% "circe-core" % Version.circe
  val circeGeneric = "io.circe" %% "circe-generic" % Version.circe
  val circeParser = "io.circe" %% "circe-parser" % Version.circe
  val circePlay = "com.dripower" %% "play-circe" % Version.playCirce
  val scalaUri = "io.lemonlabs" %% "scala-uri" % Version.scalaUri
  val sttp = "com.softwaremill.sttp.client3" %% "core" % Version.sttp
  val scalaTest = "org.scalatest" %% "scalatest" % Version.scalaTest
  val scalaTestPlusScalaCheck = "org.scalatestplus" %% "scalacheck-1-15" % Version.scalaTestPlusScalaCheck
  val scalaTestPlusPlay = "org.scalatestplus.play" %% "scalatestplus-play" % Version.scalaTestPlusPlay
  val scalaMock = "org.scalamock" %% "scalamock" % Version.scalaMock
  val scalaCheck = "org.scalacheck" %% "scalacheck" % Version.scalaCheck
  val akkaTestKit = "com.typesafe.akka" %% "akka-testkit" % Version.akkaTestKit
}
