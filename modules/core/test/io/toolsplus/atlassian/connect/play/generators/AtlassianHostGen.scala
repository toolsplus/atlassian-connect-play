package io.toolsplus.atlassian.connect.play.generators

import io.toolsplus.atlassian.connect.play.api.models.{
  AtlassianHost,
  DefaultAtlassianHost,
  DefaultAtlassianHostUser,
  JwtAuthenticationType,
  OAuth2AuthenticationType
}
import org.scalacheck.Gen
import org.scalacheck.Gen._

trait AtlassianHostGen extends SecurityContextGen {

  def connectAtlassianHostGen: Gen[DefaultAtlassianHost] =
    for {
      securityContext <- securityContextGen
      installed <- oneOf(true, false)
    } yield {
      DefaultAtlassianHost(
        securityContext.clientKey,
        securityContext.key,
        securityContext.oauthClientId,
        securityContext.sharedSecret,
        JwtAuthenticationType,
        None,
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

  def connectOnForgeAtlassianHostGen: Gen[DefaultAtlassianHost] =
    for {
      atlassianHost <- connectAtlassianHostGen
      cloudId <- alphaStr
    } yield
      atlassianHost.copy(authenticationType = OAuth2AuthenticationType,
                         cloudId = Some(cloudId))

  def anyAtlassianHostGen: Gen[AtlassianHost] =
    oneOf(connectAtlassianHostGen, connectOnForgeAtlassianHostGen)

  def atlassianHostUserGen(
      atlassianHostGen: Gen[AtlassianHost]): Gen[DefaultAtlassianHostUser] =
    for {
      atlassianHost <- atlassianHostGen
      userAccountId <- option(alphaStr)
    } yield DefaultAtlassianHostUser(atlassianHost, userAccountId)

}
