package io.toolsplus.atlassian.connect.play.generators

import io.toolsplus.atlassian.connect.play.api.models.{DefaultAtlassianHost, DefaultAtlassianHostUser}
import org.scalacheck.Gen
import org.scalacheck.Gen._

trait AtlassianHostGen extends SecurityContextGen {

  def atlassianHostGen: Gen[DefaultAtlassianHost] =
    for {
      securityContext <- securityContextGen
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
        securityContext.productType,
        securityContext.description,
        securityContext.serviceEntitlementNumber,
        installed
      )
    }

  def atlassianHostUserGen: Gen[DefaultAtlassianHostUser] =
    for {
      atlassianHost <- atlassianHostGen
      userAccountId <- option(alphaStr)
    } yield DefaultAtlassianHostUser(atlassianHost, userAccountId)

}
