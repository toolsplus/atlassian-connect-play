package io.toolsplus.atlassian.connect.play.models

import io.toolsplus.atlassian.connect.play.api.models.AtlassianHost

object Implicits {

  import scala.language.implicitConversions

  /**
    * Implicitly convert an instance of [[LifecycleEvent]] to an instance of
    * [[io.toolsplus.atlassian.connect.play.api.models.AtlassianHost]].
    *
    * @param e Lifecycle event instance.
    * @return Atlassian host instance extracted from lifecycle event.
    */
  implicit def installedEventToAtlassianHost(
      implicit e: InstalledEvent): AtlassianHost =
    AtlassianHost(
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
