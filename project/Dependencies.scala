import sbt.*

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
    Library.nimbusJoseJwt,
    Library.scalaTest % "test",
    Library.scalaTestPlusScalaCheck % "test",
    Library.scalaTestPlusPlay % "test",
    Library.scalaMock % "test",
    Library.scalaCheck % "test",
    Library.atlassianJwtGenerators % "test",
    Library.pekkoTestKit % "test"
  )
}

object Version {
  val atlassianJwt = "0.4.0"
  val cats = "2.13.0"
  val circe = "0.14.14"
  val playCirce = "3014.1"
  val scalaUri = "4.2.0"
  val sttp = "3.11.0"
  val nimbusJoseJwt = "10.5"
  val scalaTest = "3.2.19"
  val scalaTestPlusPlay = "7.0.2"
  val scalaTestPlusScalaCheck = "3.2.18.0"
  val scalaMock = "7.4.1"
  val scalaCheck = "1.18.1"
  val pekkoTestKit = "1.0.3"
}

object Library {
  val atlassianJwtGenerators =
    "io.toolsplus" %% "atlassian-jwt-generators" % Version.atlassianJwt
  val atlassianJwt =
    "io.toolsplus" %% "atlassian-jwt-core" % Version.atlassianJwt
  val cats = "org.typelevel" %% "cats-core" % Version.cats
  val circeCore = "io.circe" %% "circe-core" % Version.circe
  val circeGeneric = "io.circe" %% "circe-generic" % Version.circe
  val circeParser = "io.circe" %% "circe-parser" % Version.circe
  val circePlay = "com.dripower" %% "play-circe" % Version.playCirce
  val scalaUri = "com.indoorvivants" %% "scala-uri" % Version.scalaUri
  val sttp = "com.softwaremill.sttp.client3" %% "core" % Version.sttp
  val nimbusJoseJwt = "com.nimbusds" % "nimbus-jose-jwt" % Version.nimbusJoseJwt
  val scalaTest = "org.scalatest" %% "scalatest" % Version.scalaTest
  val scalaTestPlusScalaCheck =
    "org.scalatestplus" %% "scalacheck-1-17" % Version.scalaTestPlusScalaCheck
  val scalaTestPlusPlay =
    "org.scalatestplus.play" %% "scalatestplus-play" % Version.scalaTestPlusPlay
  val scalaMock = "org.scalamock" %% "scalamock" % Version.scalaMock
  val scalaCheck = "org.scalacheck" %% "scalacheck" % Version.scalaCheck
  val pekkoTestKit =
    "org.apache.pekko" %% "pekko-testkit" % Version.pekkoTestKit
}
