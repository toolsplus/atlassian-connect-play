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
        securityContext.oauthClientId,
        securityContext.installationId,
        securityContext.sharedSecret,
        securityContext.baseUrl,
        securityContext.displayUrl,
        securityContext.displayUrlServicedeskHelpCenter,
        securityContext.productType,
        securityContext.description,
        securityContext.serviceEntitlementNumber,
        securityContext.entitlementId,
        securityContext.entitlementNumber,
        installed
      )
    }

  def atlassianHostUserGen: Gen[DefaultAtlassianHostUser] =
    for {
      atlassianHost <- atlassianHostGen
      userAccountId <- option(alphaStr)
    } yield DefaultAtlassianHostUser(atlassianHost, userAccountId)

}
