import ReleaseTransformations._

val commonSettings = Seq(
  organization := "io.toolsplus",
  scalaVersion := "2.12.6",
  resolvers ++= Seq(
    "Typesafe repository releases" at "http://repo.typesafe.com/typesafe/releases/",
    "Bintary JCenter" at "http://jcenter.bintray.com"
  )
)

val scoverageSettings = Seq(
  coverageExcludedPackages := """.*controllers\..*Reverse.*;.*Routes.*;"""
)

lazy val publishSettings = Seq(
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  homepage := Some(url("https://github.com/toolsplus/atlassian-connect-play")),
  licenses := Seq(
    "Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ =>
    false
  },
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
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
              "tobias.binna@toolsplus.ch",
              url("https://twitter.com/tbinna"))
  )
)

lazy val noPublishSettings = Seq(
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
  setNextVersion,
  commitNextVersion,
  releaseStepCommand("sonatypeReleaseAll"),
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
  .settings(commonSettings: _*)
  .settings(noPublishSettings)

lazy val `atlassian-connect-play-api` = project
  .in(file("modules/api"))
  .settings(commonSettings: _*)
  .settings(publishSettings)
  .settings(moduleSettings(project))

lazy val `atlassian-connect-play-core` = project
  .in(file("modules/core"))
  .enablePlugins(PlayScala)
  .settings(libraryDependencies ++= Dependencies.core)
  .settings(libraryDependencies ++= Seq(ws, guice))
  .settings(commonSettings: _*)
  .settings(scoverageSettings: _*)
  .settings(publishSettings)
  .settings(moduleSettings(project))
  .dependsOn(`atlassian-connect-play-api`)
