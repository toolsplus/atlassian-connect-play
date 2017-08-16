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
    } yield {
      AtlassianHost(
        securityContext.clientKey,
        securityContext.key,
        securityContext.publicKey,
        securityContext.oauthClientId,
        securityContext.sharedSecret,
        securityContext.serverVersion,
        securityContext.pluginsVersion,
        securityContext.baseUrl,
        securityContext.productType,
        securityContext.description,
        securityContext.serviceEntitlementNumber,
        installed
      )
    }

  def atlassianHostUserGen: Gen[AtlassianHostUser] =
    for {
      atlassianHost <- atlassianHostGen
      userKey <- option(alphaStr)
    } yield AtlassianHostUser(atlassianHost, userKey)

  def maybeAtlassianHostUserGen: Gen[Option[AtlassianHostUser]] =
    option(atlassianHostUserGen)

}
