package io.toolsplus.atlassian.connect.play.api.models

import io.toolsplus.atlassian.connect.play.api.models.Predefined.ClientKey

/** Default case class implementation of [[AtlassianHost]].
  */
case class DefaultAtlassianHost(clientKey: ClientKey,
                                key: String,
                                publicKey: String,
                                oauthClientId: Option[String],
                                sharedSecret: String,
                                serverVersion: String,
                                pluginsVersion: String,
                                baseUrl: String,
                                productType: String,
                                description: String,
                                serviceEntitlementNumber: Option[String],
                                installed: Boolean) extends AtlassianHost {

  override def uninstalled: AtlassianHost = copy(installed = false)
}
