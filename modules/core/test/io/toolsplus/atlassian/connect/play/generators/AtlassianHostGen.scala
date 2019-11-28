package io.toolsplus.atlassian.connect.play.generators

import io.toolsplus.atlassian.connect.play.api.models.{
  AtlassianHostUser,
  DefaultAtlassianHost,
  DefaultAtlassianHostUser
}
import org.scalacheck.Gen
import org.scalacheck.Gen._

trait AtlassianHostGen extends SecurityContextGen with UrlGen {

  def atlassianHostGen: Gen[DefaultAtlassianHost] =
    for {
      securityContext <- securityContextGen
      displayUrl <- option(urlGen)
      displayUrlServicedeskHelpCenter <- option(urlGen)
      installed <- oneOf(true, false)

    } yield {
      DefaultAtlassianHost(
        securityContext.clientKey,
        securityContext.key,
        securityContext.publicKey,
        securityContext.oauthClientId,
        securityContext.sharedSecret,
        securityContext.serverVersion,
        securityContext.pluginsVersion,
        securityContext.baseUrl,
        displayUrl,
        displayUrlServicedeskHelpCenter,
        securityContext.productType,
        securityContext.description,
        securityContext.serviceEntitlementNumber,
        installed
      )
    }

  def atlassianHostUserGen: Gen[DefaultAtlassianHostUser] =
    for {
      atlassianHost <- atlassianHostGen
      userKey <- option(alphaStr)
      userAccountId <- option(alphaStr)
    } yield DefaultAtlassianHostUser(atlassianHost, userKey, userAccountId)

  def maybeAtlassianHostUserGen: Gen[Option[AtlassianHostUser]] =
    option(atlassianHostUserGen)

}
