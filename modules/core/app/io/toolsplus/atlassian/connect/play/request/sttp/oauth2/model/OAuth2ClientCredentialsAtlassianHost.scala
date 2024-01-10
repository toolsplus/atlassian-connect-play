package io.toolsplus.atlassian.connect.play.request.sttp.oauth2.model

import io.toolsplus.atlassian.connect.play.api.models.Predefined.ClientKey
import io.toolsplus.atlassian.connect.play.api.models.{
  AtlassianHost,
  AuthenticationType,
  DefaultAtlassianHost,
  OAuth2AuthenticationType
}

/**
  * Atlassian host model for OAuth2 enabled installations.
  *
  * Use [[OAuth2ClientCredentialsAtlassianHostValidator.validate()]] to construct an instance of this type from an
  * [[io.toolsplus.atlassian.connect.play.api.models.AtlassianHost]] instance.
  */
case class OAuth2ClientCredentialsAtlassianHost(
    clientKey: ClientKey,
    key: String,
    oauthClientId: String,
    sharedSecret: String,
    cloudId: String,
    baseUrl: String,
    displayUrl: String,
    displayUrlServicedeskHelpCenter: String,
    productType: String,
    description: String,
    serviceEntitlementNumber: Option[String],
    entitlementId: Option[String],
    entitlementNumber: Option[String],
    installed: Boolean
) {
  val authenticationType: AuthenticationType = OAuth2AuthenticationType

  def toAtlassianHost: AtlassianHost =
    DefaultAtlassianHost(
      clientKey,
      key,
      Some(oauthClientId),
      sharedSecret,
      authenticationType,
      Some(cloudId),
      baseUrl: String,
      displayUrl: String,
      displayUrlServicedeskHelpCenter: String,
      productType: String,
      description: String,
      serviceEntitlementNumber: Option[String],
      entitlementId: Option[String],
      entitlementNumber: Option[String],
      installed: Boolean
    )
}
