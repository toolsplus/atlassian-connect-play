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
    Library.scalaTestPlusPlay % "test",
    Library.scalaMock % "test",
    Library.scalaCheck % "test",
    Library.atlassianJwtGenerators % "test"
  )
}

object Version {
  val atlassianJwt = "0.1.0"
  val cats = "0.9.0"
  val circe = "0.8.0"
  val playCirce = "2608.4"
  val scalaUri = "0.4.16"
  val scalaTestPlusPlay = "3.1.1"
  val scalaMock = "3.6.0"
  val scalaCheck = "1.13.5"
}

object Library {
  val atlassianJwtGenerators = "io.toolsplus" %% "atlassian-jwt-generators" % Version.atlassianJwt
  val atlassianJwt = "io.toolsplus" %% "atlassian-jwt-core" % Version.atlassianJwt
  val cats = "org.typelevel" %% "cats-core" % Version.cats
  val circeCore = "io.circe" %% "circe-core" % Version.circe
  val circeGeneric = "io.circe" %% "circe-generic" % Version.circe
  val circeParser = "io.circe" %% "circe-parser" % Version.circe
  val circePlay = "play-circe" %% "play-circe" % Version.playCirce
  val scalaUri = "com.netaporter" %% "scala-uri" % Version.scalaUri
  val scalaTestPlusPlay = "org.scalatestplus.play" %% "scalatestplus-play" % Version.scalaTestPlusPlay
  val scalaMock = "org.scalamock" %% "scalamock-scalatest-support" % Version.scalaMock
  val scalaCheck = "org.scalacheck" %% "scalacheck" % Version.scalaCheck
}