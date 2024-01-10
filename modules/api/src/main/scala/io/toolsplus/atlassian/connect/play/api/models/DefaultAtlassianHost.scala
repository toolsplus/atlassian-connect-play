package io.toolsplus.atlassian.connect.play.api.models

import io.toolsplus.atlassian.connect.play.api.models.Predefined.ClientKey

/** Default case class implementation of [[AtlassianHost]].
 */
case class DefaultAtlassianHost(clientKey: ClientKey,
                                key: String,
                                oauthClientId: Option[String],
                                sharedSecret: String,
                                authenticationType: AuthenticationType,
                                cloudId: Option[String],
                                baseUrl: String,
                                displayUrl: String,
                                displayUrlServicedeskHelpCenter: String,
                                productType: String,
                                description: String,
                                serviceEntitlementNumber: Option[String],
                                entitlementId: Option[String],
                                entitlementNumber: Option[String],
                                installed: Boolean) extends AtlassianHost {

  override def uninstalled: AtlassianHost = copy(installed = false)
}
