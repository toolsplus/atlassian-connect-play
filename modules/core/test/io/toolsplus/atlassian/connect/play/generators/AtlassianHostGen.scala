package io.toolsplus.atlassian.connect.play.generators

import io.toolsplus.atlassian.connect.play.api.models.{AtlassianHostUser, StandardAtlassianHost, StandardAtlassianHostUser}
import org.scalacheck.Gen
import org.scalacheck.Gen._

trait AtlassianHostGen extends SecurityContextGen {

  def atlassianHostGen: Gen[StandardAtlassianHost] =
    for {
      securityContext <- securityContextGen
      installed <- oneOf(true, false)
    } yield {
      StandardAtlassianHost(
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

  def atlassianHostUserGen: Gen[StandardAtlassianHostUser] =
    for {
      atlassianHost <- atlassianHostGen
      userKey <- option(alphaStr)
    } yield StandardAtlassianHostUser(atlassianHost, userKey)

  def maybeAtlassianHostUserGen: Gen[Option[AtlassianHostUser]] =
    option(atlassianHostUserGen)

}
