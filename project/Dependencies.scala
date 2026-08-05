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

  // Play 3.0.11 still resolves vulnerable Jackson, LZ4, and Logback releases.
  // Keep these explicit so the fixed versions are included in the published
  // POM. Remove them once Play provides the fixed versions or newer.
  val securityOverrides = Seq(
    "at.yawk.lz4" % "lz4-java" % Version.lz4,
    "ch.qos.logback" % "logback-classic" % Version.logback,
    "com.fasterxml.jackson.core" % "jackson-annotations" % Version.jackson,
    "com.fasterxml.jackson.core" % "jackson-core" % Version.jackson,
    "com.fasterxml.jackson.core" % "jackson-databind" % Version.jackson,
    "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor" % Version.jackson,
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % Version.jackson,
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % Version.jackson,
    "com.fasterxml.jackson.module" % "jackson-module-parameter-names" % Version.jackson,
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % Version.jackson
  )
}

object Version {
  val atlassianJwt = "0.4.2"
  val cats = "2.13.0"
  val circe = "0.14.15"
  val playCirce = "3014.1"
  val scalaUri = "4.2.0"
  val sttp = "3.11.0"
  val nimbusJoseJwt = "10.8"
  val jackson = "2.18.9"
  val logback = "1.5.34"
  val lz4 = "1.11.1"
  val scalaTest = "3.2.19"
  val scalaTestPlusPlay = "7.0.2"
  val scalaTestPlusScalaCheck = "3.2.18.0"
  val scalaMock = "7.5.5"
  val scalaCheck = "1.19.0"
  val pekko = "1.0.3"
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
    "org.apache.pekko" %% "pekko-testkit" % Version.pekko
}
