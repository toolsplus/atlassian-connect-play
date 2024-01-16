package io.toolsplus.atlassian.connect.play.api.models

import io.toolsplus.atlassian.connect.play.api.models.Predefined.ClientKey

/**
  * Default case class implementation of [[ForgeInstallation]]
  *
  * @param installationId Unique identifier of a Forge installation
  * @param clientKey Identifying key for the Atlassian host instance that the app was installed into
  */
case class DefaultForgeInstallation(installationId: String,
                                    clientKey: ClientKey)
    extends ForgeInstallation
