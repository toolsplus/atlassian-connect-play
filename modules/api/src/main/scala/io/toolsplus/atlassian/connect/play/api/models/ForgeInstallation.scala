package io.toolsplus.atlassian.connect.play.api.models

import io.toolsplus.atlassian.connect.play.api.models.Predefined.ClientKey

/** Forge installation record where the host has migrated to Connect on Forge.
  *
  * This primarily maps the Forge installation id to an [[AtlassianHost]]'s client key. It is worth noting that
  * different installation ids may point to the same [[AtlassianHost]] record.
  */
trait ForgeInstallation {

  /**
    * Unique identifier of a Forge installation.
    *
    * @return Forge installation id.
    */
  def installationId: String

  /** Identifying key for the Atlassian host instance that the app was installed into.
    *
    * @return Client key for this product instance.
    */
  def clientKey: ClientKey
}
