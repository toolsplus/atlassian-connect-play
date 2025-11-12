package io.toolsplus.atlassian.connect.play.models

import io.toolsplus.atlassian.connect.play.api.models.{
  AtlassianHost,
  DefaultAtlassianHost
}

object Implicits {

  /** Implicitly convert an instance of [[LifecycleEvent]] to an instance of
    * AtlassianHost.
    *
    * @param e
    *   Lifecycle event instance.
    * @return
    *   Atlassian host instance extracted from lifecycle event.
    */
  implicit def installedEventToAtlassianHost(implicit
      e: InstalledEvent
  ): AtlassianHost =
    DefaultAtlassianHost(
      e.clientKey,
      e.key,
      e.oauthClientId,
      e.installationId,
      e.sharedSecret,
      e.baseUrl,
      e.displayUrl,
      e.displayUrlServicedeskHelpCenter,
      e.productType,
      e.description,
      e.serviceEntitlementNumber,
      e.entitlementId,
      e.entitlementNumber,
      installed = true,
      ttl = None
    )

}
