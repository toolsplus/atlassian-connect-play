import ReleaseTransformations.*
import xerial.sbt.Sonatype.sonatypeCentralHost

val commonSettings = Seq(
  organization := "io.toolsplus",
  scalaVersion := "2.13.16",
  versionScheme := Some("early-semver"),
  resolvers ++= Seq(
    Resolver.typesafeRepo("releases"),
    Resolver.jcenterRepo
  )
)

val scoverageSettings = Seq(
  coverageExcludedPackages := """.*controllers\..*Reverse.*;.*Routes.*;"""
)

lazy val publishSettings = Seq(
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  homepage := Some(url("https://github.com/toolsplus/atlassian-connect-play")),
  licenses := Seq(
    "Apache 2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0")),
  publishMavenStyle := true,
  Test / publishArtifact := false,
  pomIncludeRepository := { _ =>
    false
  },
  ThisBuild / publishTo := sonatypePublishToBundle.value,
  ThisBuild / sonatypeCredentialHost := sonatypeCentralHost,
  autoAPIMappings := true,
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/toolsplus/atlassian-connect-play"),
      "scm:git:git@github.com:toolsplus/atlassian-connect-play.git"
    )
  ),
  developers := List(
    Developer("tbinna",
              "Tobias Binna",
              "tobias.binna@toolsplus.io",
              url("https://twitter.com/tbinna"))
  )
)

lazy val noPublishSettings = Seq(
  publish / skip := true,
  publish := {},
  publishLocal := {},
  publishArtifact := false,
  publishTo := Some(
    Resolver.file("Unused transient repository", file("target/dummyrepo")))
)

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommand("publishSigned"),
  releaseStepCommand("sonatypeBundleRelease"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)

def moduleSettings(project: Project) = {
  Seq(
    description := project.id.split("-").map(_.capitalize).mkString(" "),
    name := project.id
  )
}

lazy val `atlassian-connect-play` = project
  .in(file("."))
  .aggregate(
    `atlassian-connect-play-api`,
    `atlassian-connect-play-core`
  )
  .settings(commonSettings)
  .settings(noPublishSettings)

lazy val `atlassian-connect-play-api` = project
  .in(file("modules/api"))
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(moduleSettings(project))

lazy val `atlassian-connect-play-core` = project
  .in(file("modules/core"))
  .enablePlugins(PlayScala)
  .settings(libraryDependencies ++= Dependencies.core)
  .settings(libraryDependencies ++= Seq(ws, guice))
  .settings(commonSettings)
  .settings(scoverageSettings)
  .settings(publishSettings)
  .settings(moduleSettings(project))
  .dependsOn(`atlassian-connect-play-api`)
