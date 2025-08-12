package io.toolsplus.atlassian.connect.play.api.models

import io.toolsplus.atlassian.connect.play.api.models.Predefined.ClientKey

import java.time.Instant

/** Default case class implementation of [[AtlassianHost]].
  */
case class DefaultAtlassianHost(
    clientKey: ClientKey,
    key: String,
    oauthClientId: Option[String],
    installationId: Option[String],
    sharedSecret: String,
    baseUrl: String,
    displayUrl: String,
    displayUrlServicedeskHelpCenter: String,
    productType: String,
    description: String,
    serviceEntitlementNumber: Option[String],
    entitlementId: Option[String],
    entitlementNumber: Option[String],
    installed: Boolean,
    ttl: Option[Instant]
) extends AtlassianHost {

  override def uninstalled(ttl: Option[Instant]): AtlassianHost =
    copy(installed = false, ttl = ttl)
}
