package io.toolsplus.atlassian.connect.play.models

import io.toolsplus.atlassian.connect.play.api.models.{AtlassianHost, DefaultAtlassianHost}

object Implicits {

  import scala.language.implicitConversions

  /**
    * Implicitly convert an instance of [[LifecycleEvent]] to an instance of
    * AtlassianHost.
    *
    * @param e Lifecycle event instance.
    * @return Atlassian host instance extracted from lifecycle event.
    */
  implicit def installedEventToAtlassianHost(
      implicit e: InstalledEvent): AtlassianHost =
    DefaultAtlassianHost(
      e.clientKey,
      e.key,
      e.publicKey,
      e.oauthClientId,
      e.sharedSecret,
      e.serverVersion,
      e.pluginsVersion,
      e.baseUrl,
      e.productType,
      e.description,
      e.serviceEntitlementNumber,
      installed = true
    )

}
