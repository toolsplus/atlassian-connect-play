package io.toolsplus.atlassian.connect.play.generators

import io.toolsplus.atlassian.connect.play.api.models.{
  AtlassianHost,
  AtlassianHostUser
}
import org.scalacheck.Gen
import org.scalacheck.Gen._

trait AtlassianHostGen extends SecurityContextGen {

  def atlassianHostGen: Gen[AtlassianHost] =
    for {
      securityContext <- securityContextGen
      installed <- oneOf(true, false)
    } yield
      AtlassianHost(
        securityContext._2,
        securityContext._1,
        securityContext._3,
        securityContext._4,
        securityContext._5,
        securityContext._6,
        securityContext._7,
        securityContext._8,
        securityContext._9,
        securityContext._10,
        securityContext._11,
        installed
      )

  def atlassianHostUserGen: Gen[AtlassianHostUser] =
    for {
      atlassianHost <- atlassianHostGen
      userKey <- option(alphaStr)
    } yield AtlassianHostUser(atlassianHost, userKey)

  def maybeAtlassianHostUserGen: Gen[Option[AtlassianHostUser]] =
    option(atlassianHostUserGen)

}
